import type { ReservationConflictSlotDto, ReservationDto, UserVO } from '../types';

export type ApplicantRole = 'teacher' | 'student';
export type ReservationDecisionType = 'course' | 'research' | 'personal';

export interface ConflictRankedReservation {
  id: number;
  reservationNo: string;
  applicantName: string;
  applicantRole: ApplicantRole;
  reservationType: ReservationDecisionType;
  creditScore: number | null;
  submitTime: string;
  status: number;
  usagePurpose?: string;
  courseOrProjectName?: string;
  priorityRank: number;
  rankLabel: string;
  rankReason: string;
  isRecommended: boolean;
}

export interface ConflictGroupViewModel {
  key: string;
  labId: number;
  labName: string;
  reservationDate: string;
  periodId: number;
  periodName: string;
  startTime?: string;
  endTime?: string;
  conflictCount: number;
  reservations: ConflictRankedReservation[];
}

export interface ReservationRowViewModel {
  id: number;
  reservationNo: string;
  labId: number;
  labName: string;
  applicantName: string;
  applicantRole: ApplicantRole;
  reservationType: ReservationDecisionType;
  creditScore: number | null;
  submitTime: string;
  reservationDate: string;
  periodName: string;
  timeDetail: string;
  status: number;
  hasConflict: boolean;
  isRecommended: boolean;
  priorityLabel: string;
  rankReason: string;
  usagePurpose?: string;
  courseOrProjectName?: string;
}

const TYPE_PRIORITY: Record<ReservationDecisionType, number> = {
  course: 1,
  research: 2,
  personal: 3,
};

export function getDecisionType(reservationType?: number): ReservationDecisionType {
  if (reservationType === 1) {
    return 'course';
  }
  if (reservationType === 2) {
    return 'research';
  }
  return 'personal';
}

export function getApplicantRole(reservationType?: number): ApplicantRole {
  return reservationType === 3 ? 'student' : 'teacher';
}

export function reservationTypeLabel(type: ReservationDecisionType): string {
  if (type === 'course') return '课程实验';
  if (type === 'research') return '教师科研';
  return '学生个人';
}

export function reservationTypeTagClass(type: ReservationDecisionType): string {
  if (type === 'course') return 'type-tag type-course';
  if (type === 'research') return 'type-tag type-research';
  return 'type-tag type-personal';
}

export function applicantRoleLabel(role: ApplicantRole): string {
  return role === 'teacher' ? '教师' : '学生';
}

export function applicantRoleTagClass(role: ApplicantRole): string {
  return role === 'teacher' ? 'role-tag role-teacher' : 'role-tag role-student';
}

export function statusLabel(status: number): string {
  if (status === 2) return '已通过';
  if (status === 3) return '已驳回';
  if (status === 4) return '已取消';
  if (status === 5) return '已完成';
  return '待审核';
}

export function statusTagClass(status: number): string {
  if (status === 2) return 'status-tag status-approved';
  if (status === 3) return 'status-tag status-rejected';
  if (status === 4) return 'status-tag status-cancelled';
  if (status === 5) return 'status-tag status-completed';
  return 'status-tag status-pending';
}

export function priorityLabel(type: ReservationDecisionType): string {
  if (type === 'course') return '最高优先级';
  if (type === 'research') return '次优先级';
  return '学生队列';
}

function toTimestamp(value?: string): number {
  if (!value) return Number.MAX_SAFE_INTEGER;
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? Number.MAX_SAFE_INTEGER : time;
}

function compareReservations(left: ConflictRankedReservation, right: ConflictRankedReservation): number {
  const typeCompare = TYPE_PRIORITY[left.reservationType] - TYPE_PRIORITY[right.reservationType];
  if (typeCompare !== 0) {
    return typeCompare;
  }

  if (left.reservationType === 'personal' && right.reservationType === 'personal') {
    const creditCompare = (right.creditScore ?? -1) - (left.creditScore ?? -1);
    if (creditCompare !== 0) {
      return creditCompare;
    }
  }

  const submitCompare = toTimestamp(left.submitTime) - toTimestamp(right.submitTime);
  if (submitCompare !== 0) {
    return submitCompare;
  }

  return left.id - right.id;
}

function buildRankReason(item: ConflictRankedReservation, sorted: ConflictRankedReservation[]): string {
  if (item.reservationType === 'course') {
    return '教师课程实验预约，按规则优先级最高';
  }
  if (item.reservationType === 'research') {
    return '教师科研预约，优先级仅次于课程实验';
  }

  const studentQueue = sorted.filter((entry) => entry.reservationType === 'personal');
  const queueIndex = studentQueue.findIndex((entry) => entry.id === item.id) + 1;
  const sameCreditBefore = studentQueue
    .slice(0, queueIndex - 1)
    .some((entry) => (entry.creditScore ?? -1) === (item.creditScore ?? -1));

  if (sameCreditBefore) {
    return `学生队列第 ${queueIndex} 名，信誉分相同按提交时间更早排序`;
  }
  return `学生队列第 ${queueIndex} 名，信誉分 ${item.creditScore ?? '--'} 优先`;
}

export function sortConflictReservations(reservations: ConflictRankedReservation[]): ConflictRankedReservation[] {
  const sorted = [...reservations].sort(compareReservations);
  return sorted.map((item, index) => ({
    ...item,
    priorityRank: index + 1,
    rankLabel: `#${index + 1}`,
    rankReason: buildRankReason(item, sorted),
    isRecommended: index === 0,
  }));
}

export function buildConflictGroups(
  groups: ReservationConflictSlotDto[],
  userMap: Map<number, UserVO>,
): ConflictGroupViewModel[] {
  return groups.map((group) => {
    const reservations = sortConflictReservations(
      group.reservations.map((item) => {
        const reservationType = getDecisionType(item.reservationType);
        const applicantRole = getApplicantRole(item.reservationType);
        const user = userMap.get(item.applicantUserId);
        return {
          id: item.reservationId,
          reservationNo: item.reservationNo,
          applicantName: item.applicantName,
          applicantRole,
          reservationType,
          creditScore: applicantRole === 'student' ? (user?.creditScore ?? null) : null,
          submitTime: item.createdAt,
          status: item.status,
          usagePurpose: item.usagePurpose,
          courseOrProjectName: item.courseOrProjectName,
          priorityRank: 0,
          rankLabel: '',
          rankReason: '',
          isRecommended: false,
        };
      }),
    );

    return {
      key: `${group.labId}-${group.reservationDate}-${group.periodId}`,
      labId: group.labId,
      labName: group.labName,
      reservationDate: group.reservationDate,
      periodId: group.periodId,
      periodName: group.periodName,
      startTime: group.startTime,
      endTime: group.endTime,
      conflictCount: group.conflictCount,
      reservations,
    };
  });
}

export function buildReservationRows(
  reservations: ReservationDto[],
  userMap: Map<number, UserVO>,
  conflictGroups: ConflictGroupViewModel[],
  labNameResolver: (labId: number) => string,
): ReservationRowViewModel[] {
  const conflictMap = new Map<number, ConflictRankedReservation>();
  conflictGroups.forEach((group) => {
    group.reservations.forEach((item) => {
      conflictMap.set(item.id, item);
    });
  });

  return reservations.map((item) => {
    const reservationType = getDecisionType(item.reservationType);
    const applicantRole = getApplicantRole(item.reservationType);
    const user = userMap.get(item.applicantUserId);
    const conflictEntry = conflictMap.get(item.id);
    const firstSlot = item.slots?.[0];
    const timeDetail = (item.slots ?? [])
      .map((slot) => `${slot.reservationDate} ${slot.periodName}`)
      .join('，');

    return {
      id: item.id,
      reservationNo: item.reservationNo,
      labId: item.labId,
      labName: labNameResolver(item.labId),
      applicantName: item.applicantName || `#${item.applicantUserId}`,
      applicantRole,
      reservationType,
      creditScore: applicantRole === 'student' ? (user?.creditScore ?? null) : null,
      submitTime: item.createdAt || '',
      reservationDate: firstSlot?.reservationDate || '',
      periodName: firstSlot?.periodName || '',
      timeDetail: timeDetail || '--',
      status: item.status,
      hasConflict: Boolean(conflictEntry),
      isRecommended: Boolean(conflictEntry?.isRecommended),
      priorityLabel: priorityLabel(reservationType),
      rankReason: conflictEntry
        ? conflictEntry.rankReason
        : `${priorityLabel(reservationType)}，当前无同组冲突`,
      usagePurpose: item.usagePurpose,
      courseOrProjectName: item.courseOrProjectName,
    };
  });
}
