#!/usr/bin/env python3
"""
QuickDeliver – Python ETA REST Service
Exposes POST /predict for delivery time regression prediction.
Supports Flask and fallback http.server.
"""

import os
import sys
import json
import math
import warnings
warnings.filterwarnings('ignore')

MODEL_JOBLIB = os.path.join(os.path.dirname(__file__), "delivery_eta_model.joblib")
MODEL_JSON = os.path.join(os.path.dirname(__file__), "delivery_eta_model.json")

def load_prediction_engine():
    # Attempt joblib / scikit-learn
    if os.path.exists(MODEL_JOBLIB):
        try:
            import joblib
            import pandas as pd
            data = joblib.load(MODEL_JOBLIB)
            model = data["model"]
            features = data.get("features", ['distance_km', 'hour_of_day', 'is_rush_hour'])
            print("Loaded scikit-learn model successfully.")
            def predict_eta(dist_km, hour):
                is_rush = 1.0 if (12 <= hour <= 14 or 19 <= hour <= 22) else 0.0
                df_input = pd.DataFrame([[dist_km, hour, is_rush]], columns=features)
                val = model.predict(df_input)[0]
                return max(10, int(round(val)))
            return predict_eta
        except Exception as e:
            print("Could not load joblib model:", e)
    
    # Fallback to analytical model
    def analytical_predict(dist_km, hour):
        # Base prep time: 10 mins
        # Speed: ~25 km/h in city + stops
        base_prep = 10.0
        if 12 <= hour <= 14:
            traffic_mult = 1.35
        elif 19 <= hour <= 22:
            traffic_mult = 1.50
        elif hour >= 23 or hour <= 6:
            traffic_mult = 0.85
        else:
            traffic_mult = 1.05
        transit = (dist_km * 2.4) * traffic_mult
        return max(10, int(round(base_prep + transit)))
    return analytical_predict

predict_fn = load_prediction_engine()

def compute_prediction(data):
    try:
        dist = float(data.get("distance_km", 3.0))
        hour = int(data.get("hour_of_day", 13))
        if dist < 0 or hour < 0 or hour > 23:
            return {"error": "Invalid distance_km or hour_of_day"}, 400
        
        eta = predict_fn(dist, hour)
        return {
            "status": "SUCCESS",
            "distance_km": dist,
            "hour_of_day": hour,
            "eta_minutes": eta,
            "algorithm": "Multiple Linear Regression (Ridge / OLS)",
            "unit": "minutes"
        }, 200
    except Exception as e:
        return {"error": str(e)}, 500

try:
    from flask import Flask, request, jsonify
    from flask_cors import CORS

    app = Flask(__name__)
    CORS(app)

    @app.route("/health", methods=["GET"])
    def health():
        return jsonify({
            "status": "UP",
            "service": "QuickDeliver Python ETA Service",
            "model_loaded": os.path.exists(MODEL_JOBLIB) or os.path.exists(MODEL_JSON)
        })

    @app.route("/predict", methods=["POST"])
    def predict():
        req_data = request.get_json(force=True, silent=True) or {}
        res, code = compute_prediction(req_data)
        return jsonify(res), code

    def run_flask(port=5000):
        print(f"Starting QuickDeliver Flask ETA Service on port {port}...")
        app.run(host="0.0.0.0", port=port, debug=False)

except ImportError:
    # Built-in http.server fallback if Flask is not installed
    from http.server import HTTPServer, BaseHTTPRequestHandler

    class SimpleEtaHandler(BaseHTTPRequestHandler):
        def do_OPTIONS(self):
            self.send_response(200)
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Content-Type")
            self.end_headers()

        def do_GET(self):
            if self.path == "/health":
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "UP", "service": "QuickDeliver Python ETA Service"}).encode())
            else:
                self.send_response(404)
                self.end_headers()

        def do_POST(self):
            if self.path == "/predict":
                content_length = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(content_length).decode()
                try:
                    req_data = json.loads(body) if body else {}
                except:
                    req_data = {}
                res, code = compute_prediction(req_data)
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(json.dumps(res).encode())
            else:
                self.send_response(404)
                self.end_headers()

    def run_flask(port=5000):
        print(f"Starting QuickDeliver Built-in HTTP ETA Service on port {port}...")
        server = HTTPServer(("0.0.0.0", port), SimpleEtaHandler)
        server.serve_forever()

if __name__ == "__main__":
    port = int(os.environ.get("ETA_PORT", 5000))
    run_flask(port)
