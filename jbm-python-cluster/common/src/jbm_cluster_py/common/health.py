from fastapi import APIRouter


def build_health_router(service_name: str, profile: str) -> APIRouter:
    router = APIRouter()

    @router.get("/actuator/health")
    async def health() -> dict:
        return {
            "status": "UP",
            "components": {
                "service": {
                    "status": "UP",
                    "details": {
                        "name": service_name,
                        "profile": profile,
                    },
                }
            },
        }

    return router
