import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/EvidenceCreate.css';
import { evidenceService } from '../../services/evidenceService';

const SUGGESTED_TYPES = ['Foto', 'Vídeo', 'Documento', 'Medida', 'Outro'];

export default function EvidenceCreate(){
  const navigate = useNavigate();
  const { caseId } = useParams();
    const [selectedType, setSelectedType] = useState<string>(SUGGESTED_TYPES[0]);
    const [customType, setCustomType] = useState<string>('');
    const [description, setDescription] = useState<string>('');
    const [submitting, setSubmitting] = useState(false);
    const [errors, setErrors] = useState<string | null>(null);

    const MAX_TYPE = 50;
    const MAX_DESC = 5000;

    const goBack = () => navigate(caseId ? `/cases/${caseId}/menu` : '/cases');

    const validate = (): string | null => {
      const typeValue = (selectedType === 'Outro' ? customType : selectedType).trim();
      if (!typeValue) return 'O tipo da evidência é obrigatório.';
      if (typeValue.length > MAX_TYPE) return `O tipo não pode ter mais de ${MAX_TYPE} caracteres.`;
      const desc = description.trim();
      if (!desc) return 'A descrição da evidência é obrigatória.';
      if (desc.length > MAX_DESC) return `A descrição não pode ter mais de ${MAX_DESC} caracteres.`;
      return null;
    }

    const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      setErrors(null);
      const err = validate();
      if (err) { setErrors(err); return; }
      const evidenceType = (selectedType === 'Outro' ? customType : selectedType).trim();
      setSubmitting(true);
      try {
        if (!caseId) throw new Error('Case ID ausente');
        await evidenceService.createCaseEvidence(Number(caseId), {
          evidenceType,
          evidenceDescription: description.trim(),
        });
        navigate(`/cases/${caseId}/evidences`);
      } catch (err: any) {
        setErrors(err?.message || 'Erro ao submeter evidência');
      } finally {
        setSubmitting(false);
      }
    }

    return (
      <div className="homepage-wrapper">
        <div className="back-container back-outside">
          <button className="page-btn secondary back-btn" onClick={goBack}>Voltar</button>
        </div>
        <div className="homepage-overlay">
          <div className="homepage-content" style={{ textAlign: 'left' }}>
            <h1 className="homepage-title">IDAP</h1>
            <p className="homepage-subtitle">Adicionar uma nova evidência ao caso {caseId ?? '—'}.</p>

              

            <form onSubmit={handleSubmit} style={{ marginTop: 24, maxWidth: 760 }}>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 700 }}>Tipo de Evidência</label>
              <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 16 }}>
                <select value={selectedType} onChange={(ev) => setSelectedType(ev.target.value)} style={{ padding: '10px 12px', borderRadius: 8 }}>
                  {SUGGESTED_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
                {selectedType === 'Outro' && (
                  <input placeholder="Especifique o tipo" value={customType} onChange={e => setCustomType(e.target.value)} style={{ flex: 1, padding: '10px 12px', borderRadius: 8 }} maxLength={MAX_TYPE} />
                )}
              </div>

              <label style={{ display: 'block', marginBottom: 8, fontWeight: 700 }}>Descrição</label>
              <textarea value={description} onChange={e => setDescription(e.target.value)} rows={6} style={{ width: '100%', padding: 12, borderRadius: 8 }} maxLength={MAX_DESC} />

              {errors && <div style={{ color: 'red', marginTop: 12 }}>{errors}</div>}

              <div style={{ marginTop: 20, display: 'flex', gap: 12 }}>
                <button type="submit" className="homepage-btn login-btn" disabled={submitting}>{submitting ? 'Enviando...' : 'Submeter Evidência'}</button>
                <button type="button" className="page-btn" onClick={goBack} style={{ alignSelf: 'center' }}>Cancelar</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    );
}
