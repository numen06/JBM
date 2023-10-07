select last(cumulative_energy) AS cumulative_energy,SPREAD(cumulative_energy) AS diff_cumulative_energy
from data
WHERE  time > '2023-07-10 10:00:00'
  and time <= '2023-07-11 10:00:00'
  and psn = '1682432403971005'
group by time(5m,-8h),psn fill(null)