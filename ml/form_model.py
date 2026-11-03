"""Shared nonlinear inference and availability-aware historical inputs; no database access."""
from datetime import date
from functools import lru_cache
import gzip
import hashlib
import json
import numpy as np
from xgboost import Booster
from .history_features import Event, History, FEATURE_NAMES, flip_features


def symmetric_probability(model, matrix):
    joined = np.concatenate((matrix, flip_features(matrix)))
    raw = (model.predict_proba(joined)[:,1] if hasattr(model,'predict_proba') else model.inplace_predict(joined)).astype(np.float64)
    size = len(matrix)
    return (raw[:size] + 1 - raw[size:]) / 2


class FormModel:
    def __init__(self, directory):
        self.metadata = json.loads((directory/'form-xgboost-v2.json').read_text(encoding='utf-8'))
        info = self.metadata
        if info['schemaVersion'] != 2 or info['featureNames'] != list(FEATURE_NAMES): raise ValueError('Unsupported historical feature schema')
        for filename, checksum in ((info['modelFile'],info['modelSha256']),(info['historyFile'],info['historySha256'])):
            if filename != __import__('pathlib').Path(filename).name or hashlib.sha256((directory/filename).read_bytes()).hexdigest()!=checksum:
                raise ValueError('Model artifact checksum mismatch')
        self.model = Booster(params={"nthread":4})
        self.model.load_model(directory/info['modelFile'])
        self.model.set_param({'nthread':4,'device':'cpu'})
        if self.model.num_features()!=len(FEATURE_NAMES): raise ValueError('Model feature count mismatch')
        history = json.loads(gzip.decompress((directory/info['historyFile']).read_bytes()))
        self.events = [Event(date.fromisoformat(row[0]),*row[1:]) for row in history['events']]
        self.trained_through = date.fromisoformat(info['trainedThrough'])
        self.data_through = date.fromisoformat(info['dataThrough'])

    @lru_cache(maxsize=8)
    def history(self, as_of):
        state = History()
        for event in self.events:
            if event.event_date <= as_of: state.schedule(event)
        state.advance(as_of)
        return state

    def predict(self, request):
        if request.as_of < self.trained_through: raise ValueError(f'Model was trained through {self.trained_through}')
        if request.as_of > self.data_through: raise ValueError(f'Historical data are available through {self.data_through}; select an earlier asOf')
        if any(not pair.source_a_id or not pair.source_b_id for pair in request.pairs): raise ValueError('Historical model requires source identities for every player')
        state = self.history(request.as_of)
        matrix = np.asarray([state.features(int(pair.source_a_id), int(pair.source_b_id),pair.rank_a,pair.rank_b,
            request.surface,request.best_of,request.as_of,request.category) for pair in request.pairs],dtype=np.float32)
        return symmetric_probability(self.model,matrix)
