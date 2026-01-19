from fastapi import FastAPI

from app.api.sparql_router import router

app = FastAPI()
app.include_router(router, prefix="/api/sparql")
#@app.get("/")
#def read_root() :
 #   return {"Hello": "World"}