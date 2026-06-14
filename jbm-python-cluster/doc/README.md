# Doc Application

Python implementation of `jbm-cluster-platform-doc`.

The application keeps Java-compatible service identity and routes:

- service name: `jbm-cluster-platform-doc`
- port: `9999`
- health: `/actuator/health`
- OpenAPI: `/openapi.json`, Swagger UI: `/docs`
- document paths: `/put`, `/upload`, `/get/**`, `/download/**`, `/remove/**`
- metadata paths: `/baseDoc/**`, `/baseDocGroup/**`, `/baseDocToken/**`
- WPS callback paths: `/v1/3rd/file/**`

Metadata is stored through SQLAlchemy with Java-compatible field names. File
content uses S3/MinIO in cluster profiles and filesystem fallback for local
development.
