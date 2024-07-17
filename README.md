
# अन्वेषक: (ANVESHAK)

अन्वेषक: (ANVESHAK) is a Python-based tool designed to assist users with scientific papers. It requires several dependencies, including a specific version of the Java Development Kit (JDK) and various Python libraries.

## Prerequisites

1. **Install JDK-17**: ANVESHAK requires JDK-17 to function correctly. Download and install JDK-17 from the [official Oracle website](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or use your package manager:

    - **Ubuntu/Debian**:
      ```sh
      sudo apt update
      sudo apt install openjdk-17-jdk
      ```

    - **macOS** (using Homebrew):
      ```sh
      brew update
      brew install openjdk@17
      ```

    - **Windows**: Download and install from the [Oracle website](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html).

2. **Install Python 3**: Ensure that Python 3 is installed on your system. You can download it from the [official Python website](https://www.python.org/downloads/).

## Installation

1. **Clone the Repository**:
   ```sh
   git clone https://github.com/AbhijitJowhari/ANVESHAK.git
   cd ANVESHAK
   ```

2. **Install Python Dependencies**:
   Make sure you have `pip3` installed. Then, install the required Python packages using `requirements.txt`:
   ```sh
   pip3 install -r requirements.txt
   ```
3. **Create API Keys**:
   Make sure you create API keys in the following two websites:
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

- Ensure that all required directories (`./grobid`, `./papers`, `./papers_xml`) and files (`config.json`, `create_d_reps.py`, `grobid_client_KG.py`) are in place and correctly set up before running the script.
- The script requires access to the internet to interact with external APIs.
- Make sure that the required environment variables (e.g., `GROQ_API_KEY`) are set correctly in your environment.

## Troubleshooting

- If you encounter issues related to missing Java components, ensure that JDK-17 is correctly installed and that your `JAVA_HOME` environment variable is set appropriately.
- For any Python-related errors, ensure all dependencies are installed by re-running `pip3 install -r requirements.txt`.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
