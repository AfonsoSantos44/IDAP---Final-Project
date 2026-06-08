import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/EvidenceCreate.css';
import { evidenceService } from '../../services/evidenceService';

const SUGGESTED_TYPES = ['Foto', 'Vídeo', 'Documento', 'Medida', 'Outro'];

export default function EvidenceEdit() {
  const navigate = useNavigate();
  const { caseId, evidenceId } = useParams();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<string | null>(null);

  const [selectedType, setSelectedType] = useState<string>('Foto');
  const [customType, setCustomType] = useState<string>('');
  const [description, setDescription] = useState<string>('');

  const MAX_TYPE = 50;
  const MAX_DESC = 5000;

  const goBack = () =>
    navigate(caseId ? `/cases/${caseId}/evidences` : '/cases');

  useEffect(() => {
    const loadEvidence = async () => {
      try {
        if (!evidenceId) {
          throw new Error('ID da evidência inválido.');
        }

        const evidence = await evidenceService.getEvidenceById(
          Number(evidenceId)
        );

        const type = evidence.evidenceType ?? '';
        const desc = evidence.evidenceDescription ?? '';

        if (SUGGESTED_TYPES.includes(type)) {
          setSelectedType(type);
          setCustomType('');
        } else {
          setSelectedType('Outro');
          setCustomType(type);
        }

        setDescription(desc);
      } catch (err: any) {
        setErrors(err?.message || 'Erro ao carregar evidência.');
      } finally {
        setLoading(false);
      }
    };

    loadEvidence();
  }, [evidenceId]);

  const validate = (): string | null => {
    const typeValue =
      selectedType === 'Outro'
        ? customType.trim()
        : selectedType.trim();

    if (!typeValue)
      return 'O tipo da evidência é obrigatório.';

    if (typeValue.length > MAX_TYPE)
      return `O tipo não pode ter mais de ${MAX_TYPE} caracteres.`;

    const desc = description.trim();

    if (!desc)
      return 'A descrição da evidência é obrigatória.';

    if (desc.length > MAX_DESC)
      return `A descrição não pode ter mais de ${MAX_DESC} caracteres.`;

    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    setErrors(null);

    const validationError = validate();

    if (validationError) {
      setErrors(validationError);
      return;
    }

    try {
      setSubmitting(true);

      if (!evidenceId) {
        throw new Error('ID da evidência inválido.');
      }

      const evidenceType =
        selectedType === 'Outro'
          ? customType.trim()
          : selectedType;

      await evidenceService.updateEvidence(
        Number(evidenceId),
        {
          evidenceType,
          evidenceDescription: description.trim(),
        }
      );

      navigate(`/cases/${caseId}/evidences`);
    } catch (err: any) {
      setErrors(err?.message || 'Erro ao atualizar evidência.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="homepage-wrapper">
        <div className="homepage-overlay">
          <div className="homepage-content">
            <p>A carregar evidência...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="homepage-wrapper">
      <div className="back-container back-outside">
        <button
          className="page-btn secondary back-btn"
          onClick={goBack}
        >
          Voltar
        </button>
      </div>

      <div className="homepage-overlay">
        <div
          className="homepage-content"
          style={{ textAlign: 'left' }}
        >
          <h1 className="homepage-title">IDAP</h1>

          <p className="homepage-subtitle">
            Editar evidência {evidenceId}
          </p>

          <form
            onSubmit={handleSubmit}
            style={{
              marginTop: 24,
              maxWidth: 760,
            }}
          >
            <label
              style={{
                display: 'block',
                marginBottom: 8,
                fontWeight: 700,
              }}
            >
              Tipo de Evidência
            </label>

            <div
              style={{
                display: 'flex',
                gap: 12,
                alignItems: 'center',
                marginBottom: 16,
              }}
            >
              <select
                value={selectedType}
                onChange={(e) =>
                  setSelectedType(e.target.value)
                }
                style={{
                  padding: '10px 12px',
                  borderRadius: 8,
                }}
              >
                {SUGGESTED_TYPES.map((type) => (
                  <option
                    key={type}
                    value={type}
                  >
                    {type}
                  </option>
                ))}
              </select>

              {selectedType === 'Outro' && (
                <input
                  placeholder="Especifique o tipo"
                  value={customType}
                  onChange={(e) =>
                    setCustomType(e.target.value)
                  }
                  maxLength={MAX_TYPE}
                  style={{
                    flex: 1,
                    padding: '10px 12px',
                    borderRadius: 8,
                  }}
                />
              )}
            </div>

            <label
              style={{
                display: 'block',
                marginBottom: 8,
                fontWeight: 700,
              }}
            >
              Descrição
            </label>

            <textarea
              value={description}
              onChange={(e) =>
                setDescription(e.target.value)
              }
              rows={6}
              maxLength={MAX_DESC}
              style={{
                width: '100%',
                padding: 12,
                borderRadius: 8,
              }}
            />

            {errors && (
              <div
                style={{
                  color: 'red',
                  marginTop: 12,
                }}
              >
                {errors}
              </div>
            )}

            <div
              style={{
                marginTop: 20,
                display: 'flex',
                gap: 12,
              }}
            >
              <button
                type="submit"
                className="homepage-btn login-btn"
                disabled={submitting}
              >
                {submitting
                  ? 'A guardar...'
                  : 'Guardar Alterações'}
              </button>

              <button
                type="button"
                className="page-btn"
                onClick={goBack}
                style={{ alignSelf: 'center' }}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}