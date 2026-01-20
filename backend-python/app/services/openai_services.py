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

RÈGLES STRICTES:
1. Génère SEULEMENT le contenu entre les accolades { ... }
2. N'inclus JAMAIS SELECT, PREFIX, LIMIT, ou FILTER qui ne concerne pas la langue
3. Chaque triple doit finir par un point (.)
4. Les noms propres deviennent des ressources DBpedia avec underscores: 
   - "Christopher Nolan" -> dbr:Christopher_Nolan
   - "Brad Pitt" -> dbr:Brad_Pitt
   - Remplace tous les espaces par des underscores dans les noms

5. Utilise les préfixes corrects:
   - dbo: pour les propriétés (dbo:director, dbo:starring, dbo:country, dbo:birthPlace, etc.)
   - dbr: pour les ressources (dbr:Christopher_Nolan, dbr:France, etc.)
   - rdfs: pour les labels (rdfs:label)
   - rdf: pour les types (rdf:type)

6. TOUJOURS ajouter: FILTER (lang(?label) = 'en' || lang(?label) = 'fr') pour les labels

EXEMPLES DE TRANSFORMATION:

"quels sont les films réalisés par Christopher Nolan?" ->
?film dbo:director dbr:Christopher_Nolan .
?film rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

"quels films a réalisé Christopher Nolan?" ->
?film dbo:director dbr:Christopher_Nolan .
?film rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

"films avec Brad Pitt" ->
?film dbo:starring dbr:Brad_Pitt .
?film rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

"acteurs de Titanic" ->
dbr:Titanic dbo:starring ?actor .
?actor rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

"films français" ->
?film dbo:country dbr:France .
?film rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

"villes en France" ->
?ville rdf:type dbo:City .
?ville dbo:country dbr:France .
?ville rdfs:label ?label .
FILTER (lang(?label) = 'en' || lang(?label) = 'fr')

IMPORTANT: Réponds UNIQUEMENT avec le WHERE body, pas d'explications, pas de SELECT, pas de PREFIX."""},
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
        {"role": "system", "content": """Tu es un assistant cinématographique expert. Réponds aux questions sur les films de manière concise et informative en utilisant ta base de connaissances. 

Si la question demande une liste (quels films, quels acteurs, etc.), format ta réponse comme une liste à puces.
Sois précis et utilise tes connaissances actualisées."""},
        {"role": "user", "content": sentence}
    ],
        temperature=0.7
    )
    return response.choices[0].message.content