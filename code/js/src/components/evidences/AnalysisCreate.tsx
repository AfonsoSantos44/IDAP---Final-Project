import React from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/AnalysisCreate.css';

export default function AnalysisCreate(){
  const navigate = useNavigate();
  const { caseId } = useParams();
  return (
    <div className="homepage-wrapper">
      <div className="back-container back-outside">
        <button className="homepage-btn login-btn" onClick={() => navigate(caseId ? `/cases/${caseId}/menu` : '/cases')}>Voltar</button>
      </div>
      <div className="homepage-overlay">
        <div className="homepage-content">
          <h1 className="homepage-title">IDAP</h1>
          <p className="homepage-subtitle">Análise do caso {caseId ?? '—'}</p>
        </div>
      </div>
    </div>
  );
}
