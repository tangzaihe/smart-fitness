# 动作库种子与枚举

> P0 Flyway 必须插入下列 30 条。`code` 稳定，禁止改名只改展示 `name`。  
> 日期：2026-08-23

`equipment` / `pattern` / `muscle_group` 取值见 [08-contracts.md](08-contracts.md)。

`swap_group` 相同的动作可互为 alternatives。

| code | name | muscle_group | equipment | pattern | swap_group |
|------|------|----------------|-----------|---------|------------|
| bb_back_squat | 杠铃深蹲 | QUAD | BARBELL | SQUAT | squat_quad |
| goblet_squat | 高脚杯深蹲 | QUAD | DUMBBELL | SQUAT | squat_quad |
| smith_squat | 史密斯深蹲 | QUAD | SMITH | SQUAT | squat_quad |
| leg_press_45 | 45度腿举 | QUAD | LEG_PRESS | SQUAT | squat_quad |
| walking_lunge | 步行弓步 | QUAD | DUMBBELL | LUNGE | lunge_quad |
| rd_deadlift | 罗马尼亚硬拉 | HAMSTRING | BARBELL | HINGE | hinge_post |
| db_rdl | 哑铃RDL | HAMSTRING | DUMBBELL | HINGE | hinge_post |
| hip_thrust | 臀桥/髋推 | GLUTE | BARBELL | HINGE | hinge_post |
| lying_leg_curl | 俯卧腿弯举 | HAMSTRING | MACHINE | ISOLATION | ham_iso |
| leg_extension | 腿屈伸 | QUAD | MACHINE | ISOLATION | quad_iso |
| bb_bench | 杠铃卧推 | CHEST | BARBELL | HORIZONTAL_PUSH | hpush_chest |
| db_bench | 哑铃卧推 | CHEST | DUMBBELL | HORIZONTAL_PUSH | hpush_chest |
| push_up | 俯卧撑 | CHEST | BODYWEIGHT | HORIZONTAL_PUSH | hpush_chest |
| machine_chest_press | 器械胸推 | CHEST | MACHINE | HORIZONTAL_PUSH | hpush_chest |
| ohp_bb | 杠铃肩上推 | SHOULDER | BARBELL | VERTICAL_PUSH | vpush_shoulder |
| ohp_db | 哑铃肩推 | SHOULDER | DUMBBELL | VERTICAL_PUSH | vpush_shoulder |
| lateral_raise | 侧平举 | SHOULDER | DUMBBELL | ISOLATION | shoulder_iso |
| pull_up | 引体向上 | BACK | PULLUP_BAR | VERTICAL_PULL | vpull_back |
| lat_pulldown | 高位下拉 | BACK | CABLE | VERTICAL_PULL | vpull_back |
| bb_row | 杠铃划船 | BACK | BARBELL | HORIZONTAL_PULL | hpull_back |
| seated_cable_row | 坐姿划船 | BACK | CABLE | HORIZONTAL_PULL | hpull_back |
| db_row | 单臂哑铃划船 | BACK | DUMBBELL | HORIZONTAL_PULL | hpull_back |
| bb_curl | 杠铃弯举 | BICEP | BARBELL | ISOLATION | biso |
| db_curl | 哑铃弯举 | BICEP | DUMBBELL | ISOLATION | biso |
| tricep_pushdown | 绳索下压 | TRICEP | CABLE | ISOLATION | tiso |
| plank | 平板支撑 | CORE | BODYWEIGHT | CORE | core |
| cable_crunch | 绳索卷腹 | CORE | CABLE | CORE | core |
| calf_raise | 提踵 | CALF | MACHINE | ISOLATION | calf |
| farmer_carry | 农夫行走 | CORE | DUMBBELL | CARRY | carry |
| face_pull | 面拉 | SHOULDER | CABLE | ISOLATION | shoulder_iso |

`is_stretch` 全部 false。P0 拉伸不入库。

Retrieve P0 规则分（0–1，再归一化到返回列表）：

```
score = 0.50 * equipment_match     -- code.equipment ∈ 用户 equipment（BODYWEIGHT 恒匹配）
      + 0.25 * pattern_match
      + 0.15 * muscle_match
      + 0.10 * preference_boost    -- liked +0.10，disliked -0.15（下限 0），never 直接排除
```

用户 equipment 含 BARBELL 则自由重量类 liked 标签 `free_weight` 命中：BARBELL/DUMBBELL/KETTLEBELL。
