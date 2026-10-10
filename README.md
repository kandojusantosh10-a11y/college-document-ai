# College Document AI Assistant

A local Retrieval-Augmented Generation (RAG) app for asking grounded questions about college PDFs. It uses Spring Boot, Spring AI, Google Gemini, PDFBox, and a vanilla JavaScript frontend.

## Architecture

PDF upload → PDFBox page text → chunks and metadata → Gemini embeddings → SimpleVectorStore

Question → query embedding → semantic search → relevant context → Gemini → answer and source pages

## Features

- PDF-only uploads with page-aware extraction and validation
- Gemini embeddings and semantic vector search via Spring AI
- Chat strictly instructed to use retrieved context only
- Persistent local vector data in `vectorstore/`
- Source filename/page citations, document deletion, responsive UI, and error handling

## Run it

Install Java 17+ and obtain a Gemini API key from Google AI Studio. In PowerShell:

```powershell
$env:GOOGLE_API_KEY = "your_gemini_api_key"
cd BackEnd
.\mvnw.cmd spring-boot:run
```

The API runs on `http://localhost:8080`. Serve `frontend/` with a static server such as VS Code Live Server (port 5500), then open the page. Change the one `API_BASE_URL` value in `frontend/app.js` if needed.

## API endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/documents/upload` | Upload multipart field `file` |
| GET | `/api/documents` | List processed PDFs |
| DELETE | `/api/documents/{fileName}` | Remove PDF and vectors |
| POST | `/api/chat` | Ask with `{ "question": "..." }` |

Upload a PDF then try questions such as “What is the minimum attendance requirement?” or “When are examinations held?” Questions not covered by the PDFs return a clear not-found answer.

## Folder layout

`BackEnd/` contains the Spring Boot API and RAG services. `frontend/` contains `index.html`, `style.css`, and `app.js`. `documents/` holds uploads; `vectorstore/` holds persistent vectors. The last two are Git-ignored.

## Viva notes

RAG retrieves relevant passages before generation. Embeddings represent text meaning numerically, so a vector store can find relevant text even with different wording. Chunking makes embeddings focused and sends only small, relevant context to the LLM. The threshold plus a context-only prompt reduce hallucinations. The local `SimpleVectorStore` is suitable for this demonstration; production deployments should use a managed vector database.

## Future enhancements

Authentication, document preview, richer metadata filters, streamed answers, and a production vector database.
