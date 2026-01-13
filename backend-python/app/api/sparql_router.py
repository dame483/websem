from fastapi import APIRouter

from app.models.schemas import SparqlResponse, SparqlRequest

router = APIRouter()

@router.post("/sparql",response_model=SparqlResponse)
def generate_sparql(request: SparqlRequest):
    sparql = generate_sparql(request.sentence)
    return {"sparql": sparql}
