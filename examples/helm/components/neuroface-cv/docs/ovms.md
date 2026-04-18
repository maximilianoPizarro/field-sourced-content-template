# OpenVINO Model Server (OVMS)

## Model: face-detection-retail-0005

Intel's retail face detection model optimized for edge deployment.

| Property | Value |
|----------|-------|
| **Format** | OpenVINO IR (FP16) |
| **Input** | `[1, 3, 300, 300]` (NCHW) |
| **Output** | `[1, 1, N, 7]` — `[image_id, label, confidence, x_min, y_min, x_max, y_max]` |
| **Source** | [Intel Model Zoo](https://storage.openvinotoolkit.org/repositories/open_model_zoo/2023.0/models_bin/1/face-detection-retail-0005/FP16/) |

## Deployment

OVMS runs as a custom Deployment in `neuroface` namespace with an init container that downloads the model at startup:

```yaml
initContainers:
  - name: download-model
    image: registry.access.redhat.com/ubi9/ubi-minimal:latest
    command: [sh, -c]
    args:
      - curl -sL $BASE_URL/face-detection-retail-0005.xml -o /models/.../
      - curl -sL $BASE_URL/face-detection-retail-0005.bin -o /models/.../
```

The model files (~1.2MB) are stored in an `emptyDir` volume shared with the OVMS container.

## REST API

### Health Check

```bash
curl http://neuroface-ovms.neuroface.svc:8000/v1/config
```

Response:
```json
{
  "face-detection-retail-0005": {
    "model_version_status": [{
      "version": "1",
      "state": "AVAILABLE",
      "status": {"error_code": "OK", "error_message": "OK"}
    }]
  }
}
```

### Inference (KServe v2)

```bash
curl -X POST http://neuroface-ovms.neuroface.svc:8000/v2/models/face-detection-retail-0005/infer \
  -H "Content-Type: application/json" \
  -d '{
    "inputs": [{
      "name": "data",
      "shape": [1, 3, 300, 300],
      "datatype": "FP32",
      "data": [...]
    }]
  }'
```

### gRPC

```
endpoint: neuroface-ovms.neuroface.svc:9000
```

## Configuration

The model configuration is stored in a ConfigMap `neuroface-ovms-config`:

```json
{
  "model_config_list": [{
    "config": {
      "name": "face-detection-retail-0005",
      "base_path": "/models/face-detection-retail-0005"
    }
  }]
}
```

## Monitoring

The Camel route `cv-ovms-status` polls `/v1/config` every 30 seconds and publishes the model status to Kafka topic `cv.face.detections` with `event_type: ovms_model_status`.
