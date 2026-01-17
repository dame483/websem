from fastapi import APIRouter

from app.models.schemas import SparqlResponse, SparqlRequest
from app.services.openai_services import generate_sparql as generate_sparql_query, generate_answer

router = APIRouter()

@router.post("/",response_model=SparqlResponse)
def generate_sparql(request: SparqlRequest):
    sparql = generate_sparql_query(request.sentence)
    return {"sparql": sparql}

@router.post("/answer", response_model=dict)
def answer_question(request: SparqlRequest):
    """Quand DBpedia n'a pas les résultats, génère une réponse depuis la base de connaissances du LLM"""
    answer = generate_answer(request.sentence)
    return {"answer": answer}
