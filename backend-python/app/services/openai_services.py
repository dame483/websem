from openai import OpenAI

from app.core.config import OPENAI_API_KEY

client = OpenAI(
    base_url="https://ollama-ui.pagoda.liris.cnrs.fr/api",
    api_key=OPENAI_API_KEY
)

def generate_sparql(sentence: str):
    """Génère UNIQUEMENT le corps de la requête SPARQL (la partie WHERE)"""
    response = client.chat.completions.create(
        model="llama3:70b",
        messages=[
            {"role": "system", "content": """Tu es un expert en requêtes SPARQL pour DBpedia. Tu dois générer UNIQUEMENT le corps d'une requête SPARQL (la partie WHERE).

IMPORTANT:
- Génère SEULEMENT le contenu entre les accolades { ... }
- N'inclus pas SELECT, PREFIX, ou LIMIT
- Utilise dbo: pour les propriétés (dbo:director, dbo:starring, dbo:country, etc.)
- Utilise dbr: pour les ressources (dbr:Christopher_Nolan, dbr:Titanic, etc.)
- Utilise rdfs: pour les labels (rdfs:label)
- Utilise rdf: pour les types (rdf:type)
- Chaque ligne doit finir par un point (.)
- OBLIGATOIRE: Ajoute un filtre de langue FILTER (lang(?label) = 'en' || lang(?label) = 'fr') pour les labels

EXEMPLES de corps valides:

Pour "quels films a réalisé Christopher Nolan?":
?film dbo:director dbr:Christopher_Nolan .
?film rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

Pour "quels acteurs jouent dans Titanic?":
dbr:Titanic dbo:starring ?actor .
?actor rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

Pour "quels films sont de type Film?":
?film rdf:type dbo:Film .
?film rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

Pour "donne-moi les villes françaises":
?ville rdf:type dbo:City .
?ville dbo:country dbr:France .
?ville rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

Tu dois répondre UNIQUEMENT avec le corps, chaque ligne doit finir par un point. Pas de SELECT, pas de PREFIX, pas d'explication."""},
            {"role": "user", "content": sentence}
        ],
        temperature=0
    )
    
    return response.choices[0].message.content.strip()

def generate_answer(sentence: str):
    """Quand DBpedia n'a pas les résultats, génère une réponse directement depuis la base de connaissances du LLM"""
    response = client.chat.completions.create(
        model = "llama3:70b",
        messages=[
        {"role": "system", "content": "Tu es un assistant cinématographique expert. Réponds aux questions sur les films de manière concise et informative en utilisant ta base de connaissances."},
        {"role": "user", "content": sentence}
    ],
        temperature=0.7
    )
    return response.choices[0].message.content