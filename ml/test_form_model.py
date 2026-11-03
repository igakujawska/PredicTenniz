from datetime import date
import unittest
import numpy as np
try:
    from .form_model import symmetric_probability
    from .history_features import flip_features
except ImportError:
    from ml.form_model import symmetric_probability
    from ml.history_features import flip_features

class FormModelMethodologyTest(unittest.TestCase):
    def test_symmetry_is_enforced_even_for_an_asymmetric_classifier(self):
        class Asymmetric:
            def predict_proba(self,x):
                p=(1/(1+np.exp(-x[:,0]-.4))).astype(np.float32)
                return np.column_stack((1-p,p))
        x=np.asarray([[.8]+[0.]*14,[-1.2]+[0.]*14],dtype=np.float32)
        p=symmetric_probability(Asymmetric(),x)
        q=symmetric_probability(Asymmetric(),flip_features(x))
        np.testing.assert_allclose(p+q,1,atol=1e-15)
        self.assertTrue(np.all((p>=0)&(p<=1)))
