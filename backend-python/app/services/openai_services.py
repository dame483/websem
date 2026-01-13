from openai import OpenAI

from app.core.config import OPENAI_API_KEY

client = OpenAI(OPENAI_API_KEY)

def generate_sparql(sentence:str):
    response = client.chat.completions.create(
        model = "llama3:70b",
        messages=[
        {"role": "system", "content": "Tu es un traducteur de requêtes plein texte vers SPARQL. Tu utilises principalement DBPEDIA. Tu connais des éléments de modèle suivant : <http://dbpedia.org/resource/Comedy>. Tu ne dois répondre qu'en SPARQL, aucun texte, aucune explication en sus."},
        {"role": "user", "content": sentence}
    ],
        temperature=0
    )
    return response.choices[0].message.content