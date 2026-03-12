import prisma from './prismaClient';

// ─── CRM Metadata Helpers ────────────────────────────────────────────────────

export async function getAllObjects() {
  return prisma.crmObject.findMany({ where: { isActive: true }, orderBy: { name: 'asc' } });
}

export async function getObjectByName(name: string) {
  return prisma.crmObject.findFirst({
    where: { name: { equals: name, mode: 'insensitive' }, isActive: true },
    include: { fields: { where: { isActive: true } } },
  });
}

export async function getFieldByObjectAndName(objectName: string, fieldName: string) {
  const obj = await getObjectByName(objectName);
  if (!obj) return null;
  return prisma.crmField.findFirst({
    where: { objectId: obj.id, name: { equals: fieldName, mode: 'insensitive' }, isActive: true },
  });
}

export async function getPipelineByName(name: string) {
  return prisma.pipeline.findFirst({
    where: { name: { equals: name, mode: 'insensitive' }, isActive: true },
    include: { stages: { orderBy: { sortOrder: 'asc' } } },
  });
}

export async function getRoleByName(name: string) {
  return prisma.role.findFirst({
    where: { name: { equals: name, mode: 'insensitive' }, isActive: true },
    include: { permissions: true },
  });
}

export async function getMetadataSummary(): Promise<string> {
  const objects = await prisma.crmObject.findMany({
    where: { isActive: true },
    include: { fields: { where: { isActive: true }, select: { name: true, fieldType: true } } },
  });

  const pipelines = await prisma.pipeline.findMany({
    where: { isActive: true },
    include: { stages: { orderBy: { sortOrder: 'asc' }, select: { name: true } } },
  });

  const roles = await prisma.role.findMany({ where: { isActive: true }, select: { name: true } });

  const lines: string[] = ['Current CRM Configuration:'];

  lines.push('\nObjects:');
  for (const obj of objects) {
    const fields = obj.fields.map((f) => `${f.name}(${f.fieldType})`).join(', ');
    lines.push(`  - ${obj.name}: [${fields}]`);
  }

  lines.push('\nPipelines:');
  for (const p of pipelines) {
    const stages = p.stages.map((s) => s.name).join(' → ');
    lines.push(`  - ${p.name} (${p.objectName}): ${stages}`);
  }

  lines.push('\nRoles:');
  lines.push(`  ${roles.map((r) => r.name).join(', ')}`);

  return lines.join('\n');
}
