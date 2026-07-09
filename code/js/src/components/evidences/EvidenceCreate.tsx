import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/EvidenceCreate.css';
import { evidenceService } from '../../services/evidenceService';
import { vehicleService, type VehicleOutput } from '../../services/vehicleService';
import { useAuth } from '../../context/AuthContext';

const SUGGESTED_TYPES = ['Foto', 'Documento', 'Medida', 'Outro'];

export default function EvidenceCreate() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { caseId } = useParams();

  const [selectedType, setSelectedType] = useState<string>(SUGGESTED_TYPES[0]);
  const [customType, setCustomType] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string>('');
  const [vehicles, setVehicles] = useState<VehicleOutput[]>([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState<string>('');
  const [documentFile, setDocumentFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<string | null>(null);

  const MAX_TYPE = 50;
  const MAX_DESC = 5000;
  const isPhotoEvidence = selectedType === 'Foto';
  const isDocumentEvidence = selectedType === 'Documento';

  const goBack = () => navigate(-1);

  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  };

  useEffect(() => {
    const loadVehicles = async () => {
      if (!caseId) return;

      try {
        const caseVehicles = await vehicleService.getCaseVehicles(Number(caseId));
        setVehicles(caseVehicles);
        if (caseVehicles.length > 0 && caseVehicles[0].vehicleId !== undefined) {
          setSelectedVehicleId(String(caseVehicles[0].vehicleId));
        }
      } catch (err: any) {
        setErrors(err?.message || 'Erro ao carregar veiculos do caso.');
      }
    };

    loadVehicles();
  }, [caseId]);

  useEffect(() => {
    if (!imageFile) {
      setImagePreview('');
      return;
    }

    const previewUrl = URL.createObjectURL(imageFile);
    setImagePreview(previewUrl);

    return () => URL.revokeObjectURL(previewUrl);
  }, [imageFile]);

  const validate = (): string | null => {
    const typeValue = (selectedType === 'Outro' ? customType : selectedType).trim();
    if (!typeValue) return 'O tipo da evidencia e obrigatorio.';
    if (typeValue.length > MAX_TYPE) return `O tipo nao pode ter mais de ${MAX_TYPE} caracteres.`;

    const desc = description.trim();
    if (!desc) return 'A descricao da evidencia e obrigatoria.';
    if (desc.length > MAX_DESC) return `A descricao nao pode ter mais de ${MAX_DESC} caracteres.`;

    if (isPhotoEvidence && !imageFile) {
      return 'A imagem e obrigatoria para evidencias do tipo Foto.';
    }

    if (isPhotoEvidence && !selectedVehicleId) {
      return 'Selecione o veiculo associado a foto.';
    }

    if (isDocumentEvidence && !documentFile) {
      return 'O ficheiro e obrigatorio para evidencias do tipo Documento.';
    }

    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors(null);

    const err = validate();
    if (err) {
      setErrors(err);
      return;
    }

    const evidenceType = (selectedType === 'Outro' ? customType : selectedType).trim();
    setSubmitting(true);

    try {
      if (!caseId) throw new Error('Case ID ausente');

      const createdEvidence = await evidenceService.createCaseEvidence(Number(caseId), {
        evidenceType,
        evidenceDescription: description.trim(),
      });

      if (isPhotoEvidence && imageFile && createdEvidence.evidenceId !== undefined) {
        await evidenceService.uploadEvidenceImage(
          createdEvidence.evidenceId,
          imageFile,
          Number(selectedVehicleId),
        );
      }

      navigate(`/cases/${caseId}/evidences`);
    } catch (err: any) {
      setErrors(err?.message || 'Erro ao submeter evidencia');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="homepage-wrapper">
      <div className="logout-wrapper">
        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </div>
      <div className="back-container back-outside">
        <button className="page-btn secondary back-btn" onClick={goBack}>
          Voltar
        </button>
      </div>

      <div className="homepage-overlay evidence-create-overlay">
        <div className="homepage-content evidence-create-content">
          <h1 className="homepage-title">IDAP</h1>

          <p className="homepage-subtitle">
            Adicionar uma nova evidência ao caso {caseId ?? '-'}.
          </p>

          <form onSubmit={handleSubmit} className="evidence-form">
            <label className="form-label">Tipo de Evidência</label>

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

            <label className="form-label">Descrição</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={6}
              className="form-control textarea"
              maxLength={MAX_DESC}
            />

            {isPhotoEvidence && (
              <section className="type-fields">
                <h2>Imagem da foto</h2>

                <label className="form-label">Veiculo associado</label>
                <select
                  value={selectedVehicleId}
                  onChange={(e) => setSelectedVehicleId(e.target.value)}
                  className="form-control"
                  disabled={vehicles.length === 0}
                >
                  {vehicles.length === 0 ? (
                    <option value="">Nenhum veiculo associado ao caso</option>
                  ) : (
                    vehicles.map((vehicle) => (
                      <option key={vehicle.vehicleId} value={vehicle.vehicleId}>
                        {vehicleLabel(vehicle)}
                      </option>
                    ))
                  )}
                </select>

                <label className="file-upload-box">
                  {imagePreview && (
                    <img
                      src={imagePreview}
                      alt="Preview da imagem selecionada"
                      className="upload-preview-image"
                    />
                  )}
                  <span>Selecionar imagem</span>
                  <small>
                    {imageFile
                      ? `${imageFile.name} (${Math.ceil(imageFile.size / 1024)} KB)`
                      : 'Nenhuma imagem selecionada'}
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
              <button type="submit" className="homepage-btn login-btn" disabled={submitting}>
                {submitting ? 'A enviar...' : 'Submeter Evidência'}
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

function vehicleLabel(vehicle: VehicleOutput) {
  const brandModel = [vehicle.brand, vehicle.model].filter(Boolean).join(' ');
  const plate = vehicle.licensePlate ? ` - ${vehicle.licensePlate}` : '';
  return `${brandModel || `Veiculo #${vehicle.vehicleId ?? '-'}`}${plate}`;
}
