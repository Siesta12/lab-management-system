export function reservationTypeLabel(type?: number): string {
  if (type === 1) {
    return '课程实验预约';
  }
  if (type === 2) {
    return '科研训练预约';
  }
  return '个人预约';
}

export function reservationTypeShortLabel(type?: number): string {
  if (type === 1) {
    return '课程实验';
  }
  if (type === 2) {
    return '科研训练';
  }
  return '个人预约';
}

