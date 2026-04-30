export function formatPercent(value: number): string {
  return `${value}%`;
}

export function getBadgeClass(status: string): string {
  if (['开放', '正常', '已通过', '启用', '充足', '已完成'].includes(status)) {
    return 'badge success';
  }

  if (['维护', '待审核', '预警', '待处理', '处理中', '维修中'].includes(status)) {
    return 'badge warning';
  }

  if (['关闭', '禁用', '已驳回', '已取消', '停用'].includes(status)) {
    return 'badge danger';
  }

  return 'badge neutral';
}

export function chartBars(values: number[]): string {
  return values
    .map((value, index) => `<span class="spark-bar" style="--value:${value}; --delay:${index}"></span>`)
    .join('');
}

