from fastapi import APIRouter

from app.models.schemas import SparqlResponse, SparqlRequest
from app.services.openai_services import generate_sparql as generate_sparql_query

router = APIRouter()

@router.post("/",response_model=SparqlResponse)
def generate_sparql(request: SparqlRequest):
    sparql = generate_sparql_query(request.sentence)
    return {"sparql": sparql}
