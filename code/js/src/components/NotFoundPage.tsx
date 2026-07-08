import * as React from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/NotFoundPage.css';

function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="not-found-wrapper">
      <div className="not-found-overlay">
        <div className="not-found-content">
          <h1 className="not-found-code">404</h1>

          <h2 className="not-found-title">
            Página Não Encontrada
          </h2>

          <p className="not-found-message">
            A página que procura não existe.
          </p>

          <button
            className="not-found-btn"
            onClick={() => navigate(-1)}
          >
            Voltar ao Início
          </button>
        </div>
      </div>
    </div>
  );
}

export default NotFoundPage;