import type { ReportVehicleOutput } from '../../services/analysisService';
import type { DamageOutput, VehicleOutput } from '../../services/vehicleService';

export function formatDate(value?: string | number | Date) {
  if (value === undefined || value === null || value === '') return '-';
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}

export function vehicleLabel(vehicle?: ReportVehicleOutput | VehicleOutput) {
  if (!vehicle) return 'Veículo';
  const parts = [
    [vehicle.brand, vehicle.model].filter(Boolean).join(' '),
    vehicle.licensePlate,
    vehicle.yearOfFabrication ? String(vehicle.yearOfFabrication) : null,
    vehicle.color,
  ].filter(Boolean);
  return parts.join(' - ') || `Veículo #${vehicle.vehicleId ?? '-'}`;
}

export function damageLabel(damage: DamageOutput) {
  const parts = [
    damage.contactZone,
    damage.deformationType,
    damage.direction,
  ].filter(Boolean);
  const head = parts.join(' - ') || `Dano #${damage.damageId ?? '-'}`;
  const height =
    damage.heightCm !== undefined && damage.heightCm !== null
      ? ` (${damage.heightCm}cm)`
      : '';
  return `${head}${height}`;
}

export function imageName(filePath?: string) {
  if (!filePath) return 'Imagem';
  const normalized = filePath.replaceAll('\\', '/');
  return normalized.split('/').filter(Boolean).at(-1) || filePath;
}

export function groupImagesByVehicle<T extends { vehicleId?: number }>(
  images: T[],
  vehicles: (ReportVehicleOutput | VehicleOutput)[]
) {
  const byVehicle = new Map<number, T[]>();
  images.forEach((image) => {
    if (image.vehicleId === undefined) return;
    const list = byVehicle.get(image.vehicleId) ?? [];
    list.push(image);
    byVehicle.set(image.vehicleId, list);
  });

  const vehicleIds = new Set<number>([
    ...vehicles
      .map((vehicle) => vehicle.vehicleId)
      .filter((id): id is number => id !== undefined),
    ...byVehicle.keys(),
  ]);

  return Array.from(vehicleIds).map((vehicleId) => ({
    vehicleId,
    vehicle: vehicles.find((vehicle) => vehicle.vehicleId === vehicleId),
    images: byVehicle.get(vehicleId) ?? [],
  }));
}
