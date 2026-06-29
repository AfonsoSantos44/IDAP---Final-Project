import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/EvidenceCreate.css';
import { evidenceService } from '../../services/evidenceService';

const SUGGESTED_TYPES = ['Foto', 'Documento', 'Medida', 'Outro'];

function readImageSize(file: File): Promise<{ width: number; height: number }> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const image = new Image();

    image.onload = () => {
      const size = {
        width: image.naturalWidth,
        height: image.naturalHeight,
      };
      URL.revokeObjectURL(url);
      resolve(size);
    };

    image.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('Nao foi possivel ler as dimensoes da imagem.'));
    };

    image.src = url;
  });
}

export default function EvidenceEdit() {
  const navigate = useNavigate();
  const { caseId, evidenceId } = useParams();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<string | null>(null);

  const [selectedType, setSelectedType] = useState<string>('Foto');
  const [customType, setCustomType] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [existingImagePath, setExistingImagePath] = useState<string>('');
  const [documentFile, setDocumentFile] = useState<File | null>(null);

  const MAX_TYPE = 50;
  const MAX_DESC = 5000;
  const isPhotoEvidence = selectedType === 'Foto';
  const isDocumentEvidence = selectedType === 'Documento';

  const goBack = () =>
    navigate(caseId ? `/cases/${caseId}/evidences` : '/cases');

  useEffect(() => {
    const loadEvidence = async () => {
      try {
        if (!evidenceId) {
          throw new Error('ID da evidencia invalido.');
        }

        const evidence = await evidenceService.getEvidenceById(Number(evidenceId));
        const type = evidence.evidenceType ?? '';

        if (SUGGESTED_TYPES.includes(type)) {
          setSelectedType(type);
          setCustomType('');
        } else {
          setSelectedType('Outro');
          setCustomType(type);
        }

        setDescription(evidence.evidenceDescription ?? '');

        if (type === 'Foto') {
          try {
            const image = await evidenceService.getEvidenceImage(Number(evidenceId));
            setExistingImagePath(image.filePath ?? '');
          } catch {
            setExistingImagePath('');
          }
        }
      } catch (err: any) {
        setErrors(err?.message || 'Erro ao carregar evidencia.');
      } finally {
        setLoading(false);
      }
    };

    loadEvidence();
  }, [evidenceId]);

  const validate = (): string | null => {
    const typeValue = (selectedType === 'Outro' ? customType : selectedType).trim();
    if (!typeValue) return 'O tipo da evidencia e obrigatorio.';
    if (typeValue.length > MAX_TYPE) return `O tipo nao pode ter mais de ${MAX_TYPE} caracteres.`;

    const desc = description.trim();
    if (!desc) return 'A descricao da evidencia e obrigatoria.';
    if (desc.length > MAX_DESC) return `A descricao nao pode ter mais de ${MAX_DESC} caracteres.`;

    if (isPhotoEvidence && !imageFile && !existingImagePath) {
      return 'A imagem e obrigatoria para evidencias do tipo Foto.';
    }

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
        throw new Error('ID da evidencia invalido.');
      }

      const evidenceType =
        selectedType === 'Outro'
          ? customType.trim()
          : selectedType;

      const imageSize = isPhotoEvidence && imageFile
        ? await readImageSize(imageFile)
        : null;

      await evidenceService.updateEvidence(Number(evidenceId), {
        evidenceType,
        evidenceDescription: description.trim(),
      });

      if (isPhotoEvidence && imageFile && imageSize) {
        await evidenceService.upsertEvidenceImage(Number(evidenceId), {
          filePath: `/images/${imageFile.name}`,
          width: imageSize.width,
          height: imageSize.height,
          metadata: null,
        });
      }

      navigate(`/cases/${caseId}/evidences`);
    } catch (err: any) {
      setErrors(err?.message || 'Erro ao atualizar evidencia.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="homepage-wrapper">
        <div className="homepage-overlay evidence-create-overlay">
          <div className="homepage-content">
            <p>A carregar evidencia...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="homepage-wrapper">
      <div className="back-container back-outside">
        <button className="page-btn secondary back-btn" onClick={goBack}>
          Voltar
        </button>
      </div>

      <div className="homepage-overlay evidence-create-overlay">
        <div className="homepage-content evidence-create-content">
          <h1 className="homepage-title">IDAP</h1>

          <p className="homepage-subtitle">
            Editar evidencia {evidenceId}
          </p>

          <form onSubmit={handleSubmit} className="evidence-form">
            <label className="form-label">Tipo de Evidencia</label>

            <div className="evidence-type-row">
              <select
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
                className="form-control"
              >
                {SUGGESTED_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>

              {selectedType === 'Outro' && (
                <input
                  placeholder="Especifique o tipo"
                  value={customType}
                  onChange={(e) => setCustomType(e.target.value)}
                  className="form-control grow"
                  maxLength={MAX_TYPE}
                />
              )}
            </div>

            <label className="form-label">Descricao</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={6}
              maxLength={MAX_DESC}
              className="form-control textarea"
            />

            {isPhotoEvidence && (
              <section className="type-fields">
                <h2>Imagem da foto</h2>

                <label className="file-upload-box">
                  <span>Selecionar imagem</span>
                  <small>
                    {imageFile
                      ? `${imageFile.name} (${Math.ceil(imageFile.size / 1024)} KB)`
                      : existingImagePath || 'Nenhuma imagem selecionada'}
                  </small>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => setImageFile(e.target.files?.[0] ?? null)}
                  />
                </label>
              </section>
            )}

            {isDocumentEvidence && (
              <section className="type-fields">
                <h2>Dados do documento</h2>

                <label className="file-upload-box">
                  <span>Selecionar ficheiro</span>
                  <small>
                    {documentFile
                      ? `${documentFile.name} (${Math.ceil(documentFile.size / 1024)} KB)`
                      : 'Nenhum ficheiro selecionado'}
                  </small>
                  <input
                    type="file"
                    onChange={(e) => setDocumentFile(e.target.files?.[0] ?? null)}
                  />
                </label>
              </section>
            )}

            {errors && <div className="form-error">{errors}</div>}

            <div className="form-actions">
              <button
                type="submit"
                className="homepage-btn login-btn"
                disabled={submitting}
              >
                {submitting ? 'A guardar...' : 'Guardar Alteracoes'}
              </button>

              <button type="button" className="page-btn" onClick={goBack}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
