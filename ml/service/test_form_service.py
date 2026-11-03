"""Actual deployed artifact checks at the HTTP boundary."""
from datetime import date
import unittest
from fastapi.testclient import TestClient
from ml.service.app import app

class FormServiceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.context=TestClient(app); cls.client=cls.context.__enter__()
    @classmethod
    def tearDownClass(cls): cls.context.__exit__(None,None,None)
    def request(self,**changes):
        body={'modelVersion':'form-xgboost-v2','asOf':'2026-06-08','surface':'CLAY','bestOf':5,'category':'GRAND_SLAM',
            'pairs':[{'playerAId':1,'playerBId':2,'rankA':1,'rankB':2,'sourceAId':'206173','sourceBId':'207989'}]}
        body.update(changes); return body
    def test_actual_model_is_symmetric_and_history_respects_query_date(self):
        body=self.request(); first=self.client.post('/v1/predict',json=body)
        self.assertEqual(200,first.status_code)
        pair=body['pairs'][0]
        body['pairs']=[{'playerAId':pair['playerBId'],'playerBId':pair['playerAId'],'rankA':pair['rankB'],'rankB':pair['rankA'],
            'sourceAId':pair['sourceBId'],'sourceBId':pair['sourceAId']}]
        second=self.client.post('/v1/predict',json=body)
        self.assertAlmostEqual(1,first.json()['predictions'][0]['probabilityA']+second.json()['predictions'][0]['probabilityA'],places=12)
        engine=self.client.app.state.engine.form_model
        history=engine.history(date(2025,1,1))
        self.assertTrue(all(result[0]<=date(2025,1,1) for player in history.players.values() for result in player.results))
    def test_missing_id_or_dates_outside_artifact_window_are_rejected(self):
        for date_ in ('2024-06-08','2026-06-09'):
            self.assertEqual(422,self.client.post('/v1/predict',json=self.request(asOf=date_)).status_code)
        self.assertEqual(422,self.client.post('/v1/predict',json=self.request(pairs=[{'playerAId':1,'playerBId':2,'rankA':1,'rankB':2}])).status_code)
    def test_grand_slam_sized_batch_and_unknown_history_are_supported(self):
        pairs=[{'playerAId':a,'playerBId':b,'rankA':a,'rankB':b,'sourceAId':str(900000+a),'sourceBId':str(900000+b)} for a in range(1,129) for b in range(a+1,129)]
        response=self.client.post('/v1/predict',json=self.request(pairs=pairs))
        self.assertEqual(200,response.status_code)
        self.assertEqual(8128,len(response.json()['predictions']))
        extra=pairs+[{'playerAId':1,'playerBId':129,'rankA':1,'rankB':129,'sourceAId':'900001','sourceBId':'900129'}]
        self.assertEqual(422,self.client.post('/v1/predict',json=self.request(pairs=extra)).status_code)
