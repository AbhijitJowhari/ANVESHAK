import os
import xml.etree.ElementTree as ET
import pickle
from mixedbread_ai.client import MixedbreadAI
import os
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

mxbai = MixedbreadAI(api_key=os.environ.get("MXBAI_API_KEY"))

def get_embeddings(texts, model, prompt=None):
    res = mxbai.embeddings(
        input=texts,
        model=model,
        prompt=prompt
    )
    embeddings = [entry.embedding for entry in res.data]
    return np.array(embeddings)

directory = './papers_xml'

def extract_text_from_files(directory):
    # Namespace
    ns = {'tei': 'http://www.tei-c.org/ns/1.0'}

    # List to store the heading and its corresponding content
    content_list = []

    # Iterate over all XML files in the directory
    for filename in os.listdir(directory):
        if filename.endswith('.xml'):
            # Parse the XML file
            tree = ET.parse(os.path.join(directory, filename))
            root = tree.getroot()

            # Extract the contents under each heading
            current_heading = None
            for elem in root.iter():
                if elem.tag == '{http://www.tei-c.org/ns/1.0}head':
                    if elem.text is not None:
                        current_heading = elem.text.strip()
                elif elem.tag == '{http://www.tei-c.org/ns/1.0}p' and current_heading:
                    if elem.text is not None:
                        content_list.append(elem.text.strip())

    return content_list

text_chunks = extract_text_from_files(directory)

model_name = "mixedbread-ai/mxbai-embed-2d-large-v1"
if('d_reps.npy' not in list(os.listdir("./"))):
    d_reps = []
    start = 0
    end = 100
    while(start <= len(text_chunks)):
        print(start,'\n')
        tmp = get_embeddings(text_chunks[start:end], model_name)
        d_reps.extend(tmp)
        start+=100
        end+=100

    with open('d_reps.npy','wb') as file:
        np.save(file,np.array(d_reps))
        file.close
        print("created d_reps successfully !")

with open('text_chunks.txt','w') as file:
    file.write(str(text_chunks))
    
