"""Chronological XGBoost experiment. No test-season tuning; outputs stay in target/."""
from collections import Counter
from datetime import date, timedelta
import gzip
import hashlib
import json
from pathlib import Path
import numpy as np
import xgboost
from xgboost import XGBClassifier
from .download_matches import ROOT
from .history_features import FEATURE_NAMES, EMBARGO_DAYS, build_examples, flip_features, load_events
from .train import evaluate, calibration

VERSION = 'form-xgboost-v2'
OUTPUT = ROOT / 'target/ml/form-v2'


from .form_model import symmetric_probability


def fit(matrix, parameters):
    model = XGBClassifier(**parameters, objective='binary:logistic', eval_metric='logloss',
        tree_method='hist', random_state=42, n_jobs=4, reg_lambda=10, min_child_weight=20, subsample=.9, colsample_bytree=.9)
    model.fit(np.concatenate((matrix, flip_features(matrix))), np.concatenate((np.ones(len(matrix)), np.zeros(len(matrix)))))
    return model


def main():
    events, quality, manifest = load_events()
    examples = build_examples(events)
    rows = np.asarray([x for _, x in examples], dtype=np.float32)
    dates = np.asarray([event.event_date for event,_ in examples])
    validation_start, test_start = date(2024,1,1), date(2025,1,1)
    train = dates < validation_start - timedelta(days=EMBARGO_DAYS)
    validation = (dates >= validation_start) & (dates < test_start - timedelta(days=EMBARGO_DAYS))
    final_train = dates < test_start - timedelta(days=EMBARGO_DAYS)
    test = (dates >= test_start) & (dates < date(2026,1,1))
    if not all(mask.any() for mask in (train,validation,final_train,test)): raise ValueError('Empty chronological partition')
    candidates = []
    for parameters in ({'max_depth':2,'n_estimators':120,'learning_rate':.04}, {'max_depth':3,'n_estimators':160,'learning_rate':.04}, {'max_depth':3,'n_estimators':240,'learning_rate':.03}):
        model = fit(rows[train],parameters)
        score = evaluate(symmetric_probability(model,rows[validation]))
        candidates.append({'parameters':parameters,**score})
        print({'validation':candidates[-1]},flush=True)
    selected = min(candidates,key=lambda item:item['logLoss'])
    model = fit(rows[final_train],selected['parameters'])
    probabilities = symmetric_probability(model,rows[test])
    baseline = 1 / (1 + np.exp(-.5*rows[test,0]))
    elo = 1 / (1 + np.exp(-np.log(10)*rows[test,1]))
    v1 = json.loads((ROOT/'ml/artifacts/rank-logistic-v1.json').read_text(encoding='utf-8'))
    coefficients = v1['coefficients']
    test_events = [event for (event,_),included in zip(examples,test) if included]
    logistic = np.asarray([1/(1+np.exp(-(coefficients[event.surface.lower()] + (coefficients['bestOf5'] if event.best_of==5 else 0))*x[0])) for event,x in zip(test_events,rows[test])])
    categories = {}
    for category in sorted({event.category for event in test_events}):
        mask = np.asarray([event.category==category for event in test_events])
        categories[category] = {'matches':int(mask.sum()),'model':evaluate(probabilities[mask]),'ranking':evaluate(baseline[mask])}
    # Paired bootstrap uses match-level losses, rather than treating mirrored orientations as independent.
    differences = -np.log(np.clip(probabilities,1e-12,1)) + np.log(np.clip(logistic,1e-12,1))
    rng=np.random.default_rng(42)
    samples=np.asarray([rng.choice(differences,len(differences),replace=True).mean() for _ in range(2000)])
    OUTPUT.mkdir(parents=True,exist_ok=True)
    model.save_model(OUTPUT/f'{VERSION}.ubj')
    history_payload = json.dumps({'schemaVersion':1,'events':[[event.event_date.isoformat(),event.winner,event.loser,event.winner_rank,event.loser_rank,event.surface,event.best_of,event.category] for event in events]},separators=(',',':')).encode()
    history_bytes = gzip.compress(history_payload,mtime=0)
    (OUTPUT/'history-v2.json.gz').write_bytes(history_bytes)
    importance=model.get_booster().get_score(importance_type='gain')
    total=sum(importance.values()) or 1
    metadata={'schemaVersion':2,'version':VERSION,'label':'XGBoost · Elo i forma','trainedThrough':'2024-12-31','dataThrough':'2026-06-08',
        'testYear':2025,'testMatches':int(test.sum()),'trainMatches':int(final_train.sum()),'historyMatches':len(events),
        'featureNames':list(FEATURE_NAMES),'historyFile':'history-v2.json.gz','historySha256':hashlib.sha256(history_bytes).hexdigest(),
        'modelFile':f'{VERSION}.ubj','modelSha256':hashlib.sha256((OUTPUT/f'{VERSION}.ubj').read_bytes()).hexdigest(),
        'metrics':{'ml':evaluate(probabilities),'baseline':evaluate(baseline),'coinFlip':evaluate(np.full(int(test.sum()),.5)),'logistic':evaluate(logistic),'elo':evaluate(elo)},
        'categoryScores':categories,'calibration':calibration(probabilities),'validationCandidates':candidates,'selectedParameters':selected['parameters'],
        'pairedLogLossDifferenceVsLogistic':{'mean':float(differences.mean()),'lower95':float(np.quantile(samples,.025)),'upper95':float(np.quantile(samples,.975))},
        'featureImportance':[{'feature':FEATURE_NAMES[int(key[1:])],'gainShare':value/total} for key,value in sorted(importance.items(),key=lambda item:-item[1])],
        'dataQuality':quality,'source':manifest,'availabilityAssumptionDays':EMBARGO_DAYS,'runtime':{'xgboost':xgboost.__version__},
        'notes':['Gains describe the fitted trees, not causal effects.','Historical availability uses a conservative 21-day embargo after event start.','TOUR uses no category indicator; select a known tournament format for category context.','Test results are retrospective, not a guarantee of future performance.']}
    (OUTPUT/f'{VERSION}.json').write_text(json.dumps(metadata,indent=2)+chr(10),encoding="utf-8")
    print(json.dumps({'trainMatches':metadata['trainMatches'],'testMatches':metadata['testMatches'],'historyMatches':len(events),'metrics':metadata['metrics'],'paired':metadata['pairedLogLossDifferenceVsLogistic']},indent=2),flush=True)

if __name__=='__main__': main()
