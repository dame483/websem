from pydantic import BaseModel

class SparqlRequest(BaseModel):
    sentence: str

class SparqlResponse(BaseModel):
    sparql: str
