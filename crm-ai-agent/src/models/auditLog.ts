import prisma from './prismaClient';
import type { Prisma } from '@prisma/client';

export interface CreateAuditLogInput {
  userId: string;
  instruction: string;
  generatedConfig: Record<string, unknown>;
  executionStatus: string;
  errorMessage?: string;
}

export async function createAuditLog(input: CreateAuditLogInput) {
  return prisma.configAuditLog.create({
    data: {
      userId: input.userId,
      instruction: input.instruction,
      generatedConfig: input.generatedConfig as Prisma.InputJsonValue,
      executionStatus: input.executionStatus,
      errorMessage: input.errorMessage,
    },
  });
}

export async function updateAuditLogStatus(
  id: string,
  status: string,
  errorMessage?: string,
) {
  return prisma.configAuditLog.update({
    where: { id },
    data: {
      executionStatus: status,
      errorMessage,
      executedAt: status === 'executed' || status === 'failed' ? new Date() : undefined,
    },
  });
}

export async function getAuditLogs(userId?: string, limit = 50) {
  return prisma.configAuditLog.findMany({
    where: userId ? { userId } : undefined,
    orderBy: { createdAt: 'desc' },
    take: limit,
  });
}

export async function getAuditLogById(id: string) {
  return prisma.configAuditLog.findUnique({ where: { id } });
}
