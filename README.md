# Find My Words - Backend

## Overview

This project is a Kotlin-based backend application designed to manage a set of words using Elasticsearch as a database. The primary functionality includes adding, searching, and retrieving words according to specific criteria such as language, character set, and word length, aiding in word puzzle games like Scrabble, Words with Friends and Anygram.

### Features

Public API endpoints to:
- Paginated list words in the database.
- Perform complex search operations using custom search criteria.
- Handle errors such as duplicate entries gracefully.

Admin API endpoints to:
- Add words to the database.
- Bulk insertion of words from a file.

## Requirements

- **Kotlin**: 2.0.20+
- **Ktor**: 2.3.12+
- **Elasticsearch**: 8.9.0+
- **Java**: 11 (Corretto recommended)
- **Docker** (for running Elasticsearch via TestContainers)

## Directory Structure

```plaintext
/find-my-words-backend
│
├── /src
│   ├── /main
│   │   ├── /kotlin
│   │   │   ├── /com.kivikood
│   │   │   │   ├── /domain
│   │   │   │   │   ├── SearchCriteria.kt
│   │   │   │   │   ├── WordDocument.kt
│   │   │   │   ├── /plugins
│   │   │   │   │   ├── Frameworks.kt
│   │   │   │   │   ├── Routing.kt
│   │   │   │   │   ├── Serialization.kt
│   │   │   │   ├── /services
│   │   │   │   │   ├── ElasticsearchService.kt
│   │   │   │   ├── Application.kt
│   │   │   ├── /resources
│   │   │       ├── application.conf
│   │   │       ├── logback.xml
│   ├── /test
├── .env
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── README.md
```
## How to Run Locally

### Clone the repository:

```bash
git clone https://github.com/fbandeirac/find-my-words-backend.git
cd find-my-words-backend
```
### Set up Elasticsearch:

You can either use Docker to run Elasticsearch locally or integrate it with TestContainers for testing purposes.

To run Elasticsearch via Docker:

```bash
docker run -d --name elasticsearch -p 9200:9200 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.17.10
```

### Configure .env file:

The .env file is used to store application specific environment variables. It's located in the root directory of the project. You can configure the following variables:

ELASTIC_HOST > Elasticsearch host.
ELASTIC_PORT > Elasticsearch port.
ELASTIC_SCHEME > Elasticsearch scheme.
WORD_LIST_SIZE > Number of words to return in a single request.
TOKEN > Admin token to access admin endpoints.

### Run the application:

You can run the application using your preferred IDE or via the command line:

```bash
./gradlew run
```

### Access the API:

The application exposes the following routes:

- POST /add-word: Add a word to the database. 
: Add a word to the database.

- POST /bulk/
: Bulk upload words from a file.

- POST /search-with-criteria
: Perform a search with custom criteria.

Example request to search for words:

```json
{
"minLength": 3,
"maxLength": 10,
"characterSet": ["a", "b", "c"],
"obligatoryCharacters": ["a"],
"startWith": "a",
"endWith": "c",
"language": "pt-br"
}
```

### Logging Configuration:

The project uses logback for logging. You can configure the logging level in logback.xml.

Example:

```xml
<root level="debug">
<appender-ref ref="STDOUT"/>
</root>
```

### Running Tests:

The project uses JUnit and MockK for testing. You can run tests via the command line:

```bash
./gradlew test
```

For Elasticsearch tests, TestContainers are used to automatically spin up and tear down the Elasticsearch container.

## Contact
For any inquiries or issues, feel free to [open an issue](https://github.com/fbandeirac/find-my-words-backend/issues) or contact me directly at [fbandeira.dev@gmail.com](mailto:fbandeira.dev@gmail.com)
