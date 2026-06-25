import React from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import analysisService from '../../services/analysisService';
import '../../styles/EvidenceMenu.css';

export default function EvidenceMenu() {
	const { logout } = useAuth();
	const navigate = useNavigate();
	const { caseId } = useParams();

	const handleLogout = async () => {
		await logout();
		navigate('/initial');
	}
const handleAnalyze = async () => {
  try {
    if (!caseId) return;

    const analyses =
      await analysisService.listCaseAnalyses(
        Number(caseId)
      );

    if (analyses.length > 0) {
      navigate(
        `/cases/${caseId}/analysis/${analyses[0].analysisId}`
      );
      return;
    }

    const analysis =
      await analysisService.createCaseAnalysis(
        Number(caseId)
      );

    navigate(
      `/cases/${caseId}/analysis/${analysis.analysisId}`
    );
  } catch (err: any) {
    alert(err?.message || 'Erro ao criar análise');
  }
};
	return (
		<div className="homepage-wrapper">
			<div className="back-container back-outside">
				<button className="page-btn secondary back-btn" onClick={() => navigate(caseId ? `/cases/${caseId}` : '/cases')}>Voltar</button>
			</div>
			<div className="homepage-overlay">
				<div className="logout-wrapper">
					<button className="logout-btn" onClick={handleLogout}>Logout</button>
				</div>

				<div className="homepage-content">
					<h1 className="homepage-title">IDAP</h1>
					<p className="homepage-subtitle">Gestão de Evidências</p>

                    

					<div className="homepage-buttons">
						<Link to={`/cases/${caseId ?? ''}/evidences`}>
							<button className="homepage-btn login-btn">Ver todas as Evidências</button>
						</Link>

						<Link to={`/cases/${caseId ?? ''}/evidences/create`}>
							<button className="homepage-btn login-btn">Adicionar Evidência</button>
						</Link>

						<div>
							<button className="homepage-btn login-btn" onClick={handleAnalyze}>Analisar</button>
						</div>
					</div>
				</div>
			</div>
		</div>
	);

}
