
   

   # अन्वेषक: (ANVESHAK)

   

   अन्वेषक: (ANVESHAK) is a Python-based tool designed to assist users with scientific papers. It requires several dependencies, including various Python libraries.

   

   ## Prerequisites

   1.  **Install Python 3**: Ensure that Python 3 is installed on your system. You can download it from the [official Python website](https://www.python.org/downloads/).


   ## Installation

   

   1.  **Clone the Repository**:

   ```sh

   git clone https://github.com/AbhijitJowhari/ANVESHAK.git

   cd ANVESHAK

   ```

   2.  **Install Python Dependencies**:

   Make sure you have `pip3` installed. Then, install the required Python packages using `requirements.txt`:

   ```sh

   pip3 install -r requirements.txt

   ```

   3.  **Create API Keys**:

   ANVESHAK used APIs (Application Programming Interfaces) to leverage Large Language Models for answer your scientific queries. So make sure you create API keys in the following two websites:

   > [MixedBreadAI](https://mixedbread.ai) (API_1) <br>

   > [GroqCloud](https://console.groq.com/keys) (API_2)

   Then run the following command in the terminal:

   ```sh

   export MXBAI_API_KEY="<API_1>"

   export GROQ_API_KEY="<API_2>"

   ```
   ## Running ANVESHAK

   **NOTE:** Keep your research papers in the ```/papers``` directory. Otherwise ANVESHAK will raise an error. Also create an empty directory ```papers_xml``` in the root directory of this repo. <br><br>

   To run `ANVESHAK`, you will need an authentication key which can be requested from the [author](mailto:abhijitsj22@iitk.ac.in). Once you have the authentication key, run the following command in the root directory of this repo:

   ```sh

   ./ANVESHAK

   ```

   Enter the correct auth key when prompted. This will start the ANVESHAK application, and you can interact with ANVESHAK via your web browser.


   ## Notes

   - Ensure that all required directories (`./papers`, `./papers_xml`) and files (`config.json`, `create_d_reps.py`, `grobid_client_KG.py`) are in place and correctly set up before running the script.

   - The script requires access to the internet to interact with external APIs.

   - Make sure that the required environment variables (e.g., `GROQ_API_KEY`) are set correctly in your environment.
   ## Troubleshooting


   - For any Python-related errors, ensure all dependencies are installed by re-running `pip3 install -r requirements.txt`.

   

   ## Acknowledgements

   
   I would like to acknowledge the use of the [GROBID](https://github.com/kermitt2/grobid) framework in this project. GROBID (GeneRation Of BIbliographic Data) is an open-source library for extracting, parsing, and re-structuring raw documents into structured data.
   

   ## License

   

   This project is licensed under the MIT License. See the `LICENSE` file for details.