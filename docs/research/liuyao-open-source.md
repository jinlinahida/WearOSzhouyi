# 六爻开源实现调研

调研日期：2026-07-30
目标：为本项目选择可参考的六爻/纳甲实现，并确认许可证与规则覆盖范围。本文只记录来源、结构和待核对事项；不会把第三方代码直接复制到 Android 工程中。

## 1. bopo/najia

来源：[GitHub 仓库](https://github.com/bopo/najia)、[README](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/README.md)、[测试目录](https://github.com/bopo/najia/tree/9cf119169d7eb8e48febc05274aebf3f7106d647/tests)。下述源码链接固定到 2026-07-30 核对的 HEAD `9cf119169d7eb8e48febc05274aebf3f7106d647`，避免 `master` 后续变动改变结论。

- [LICENSE](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/LICENSE) 是标准 MIT License（copyright `2019, najia`）：可使用、复制、修改、发布、再授权和销售，但副本或实质部分须保留版权与许可声明；项目不应复制代码，若将来分发实质性改写/移植，须按该条件保留归属与许可证。
- 实现将六爻字符串按**初爻至上爻**处理；`set_shi_yao` 以“寻世诀”计算世、应和世序，[`palace`](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/utils.py#L179-L212) 再按世序、游魂/归魂规则定八宫。规则不是预先列成八宫 64 卦表，而是以三爻 bit 串和 [`YAOS`/`GUAS` 常量](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/const.py#L13-L61) 推导；世应算法见 [`utils.py`](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/utils.py#L97-L140)。
- [`get_najia`](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/utils.py#L234-L254) 从内卦取 `NAJIA[*][0]`、外卦取 `NAJIA[*][1]`，合成为初至上六个干支；[`get_qin6`](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/utils.py#L257-L275) 用“卦宫五行 − 爻支五行”的模 5 差映射兄弟、父母、官鬼、妻财、子孙。六神由日干起始并循环排列（[实现](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/utils.py#L57-L80)）；旬空以干支索引算六旬之一（[实现](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/utils.py#L35-L54)），`compile` 的实际日期旬空则委托 `lunar_python` 的 `getDayXunKong()`（[调用处](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/najia.py#L54-L91)）。
- 测试覆盖度有限但可追溯：[六神](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/tests/test_god6.py)、[八宫单例](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/tests/test_gong.py)、[纳甲单例](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/tests/test_najia.py)、[六亲五行映射](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/tests/test_qin6.py) 和 [甲子六旬旬空](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/tests/test_xkong.py)。没有世应的专门测试，也没有变卦六亲的端到端测试，不能将其测试集视为完整规则验证。

### 固定输入复核：`[6, 7, 8, 9, 8, 7]`

按通常铜钱记法（6 老阴动、7 少阳静、8 少阴静、9 老阳动；顺序初至上）直接传给上游 `compile` 时，本卦仍可由 `p % 2` 推得 `010101`，即**火水未济**：离宫，世三、应六；纳甲为 `戊寅、戊辰、戊午、己酉、己未、己巳`；以离火宫计算的六亲依次为 `父母、子孙、兄弟、妻财、子孙、兄弟`。但这不是上游可正确排出变卦的公共输入：其变卦函数只认内部编码 `3/4` 为动爻（[源码](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/najia.py#L128-L155)），因此原样输入不会生成 `bian`，同时 `dong = x > 2` 又会把六个值都标为动爻。这是输入编码不兼容/实现缺陷，不应直接移植。

为验证同一卦象，须先归一为项目内部的 `[4, 1, 2, 3, 2, 1]`（6→4、7→1、8→2、9→3）。该输入的变卦为 `110001`，即**山泽损**：艮宫，世三、应六；变卦纳甲为 `丁巳、丁卯、丁丑、丙戌、丙子、丙寅`。这给 Kotlin 单测提供了不依赖日期的可复现向量。

**变卦六亲结论：源码未按变卦宫重算。** `compile` 把本卦离宫的 `gong` 传入 `_transform`，[`_transform`](https://github.com/bopo/najia/blob/9cf119169d7eb8e48febc05274aebf3f7106d647/najia/najia.py#L128-L155) 再用这个传入值计算变卦六亲。因此上述变卦实际输出仍以离火宫计：`兄弟、父母、子孙、子孙、官鬼、父母`。若按变卦自身艮土宫重新计算，则应为：`父母、官鬼、兄弟、兄弟、妻财、官鬼`。README 的修复说明称程序“按变卦所在的本宫卦”取六亲，和当前源码相反，可能是文档滞后；该项目的测试没有覆盖这个分歧。本项目必须先决定采用哪一口径，再以该固定向量编写本卦、变卦两组断言，不能把 README 文字当作已验证行为。

## 2. Sudo-Biao/suangua

来源：[GitHub 仓库](https://github.com/Sudo-Biao/suangua)。

- README 把六爻模块拆成 `divination.py`（起卦输入）、`hexagram_data.py`（六十四卦与爻辞）、`interpreter.py`（解读）和 `najia.py`（纳甲、世应、六亲、六兽），其目录说明了计算引擎与 API 层分离的组织方式。
- README 的手动输入示例使用从初爻到上爻的六个数值 `[6/7/8/9]`，并将 6/8 作为阴、7/9 作为阳，同时保留动爻信息；这与本项目的四态输入模型一致，可用来核对输入顺序与动静表达。
- README 声称完整覆盖纳甲、世应、六亲、六兽、动爻/变爻、旬空等能力，并列出《增删卜易》《卜筮正宗》《易隐》《火珠林》《黄金策》等参考典籍。这里的“完整”是项目作者声明，不等同于已经独立验证的规则，因此本项目不会把它当作唯一规范。
- README 的许可证段落声明项目使用 MIT；在正式引入任何数据或代码前，仍需对仓库当前 `LICENSE` 文件和具体数据文件逐项复核。当前阶段只参考其模块边界、字段列表和测试方向，不复制代码、卦辞文本或知识库。
- 该仓库同时包含 FastAPI、Web 前端和可选 LLM/在线服务；这些部分不符合本项目“完全离线、无服务器、无 WebView”的约束，因此只参考其纯计算层的概念划分。

## 3. 选择与限制

当前选择 `bopo/najia` 作为最直接的纳甲/六爻结构参考，选择 `Sudo-Biao/suangua` 作为模块拆分和字段覆盖的交叉参考。两者都不能单独决定本项目的流派规则。

在实现前必须明确并写成 Kotlin 单元测试的规则：

1. 八宫、卦宫五行和世应位置的完整映射，尤其是归魂、游魂卦。
2. 六亲在本卦和变卦中的取法；`bopo/najia` 的 README 明确指出这里存在实现分歧。
3. 纳甲天干地支与五行的阴阳配法，以及内外卦的起始地支顺序。
4. 六神起法（以日干还是其他口径为准）和旬空算法的历法边界。
5. 年月日时干支的历法口径（时区、立春/节气边界、真太阳时是否采用）。
6. 动爻爻辞的选择规则，以及没有动爻时结果模型/UI 的空值行为。

本项目已将上述确认后的规则以独立 Kotlin 规则对象和单元测试固化：
`HexagramRules` 负责八宫/世应，`NajiaRules` 负责纳甲，`SixRelationRules`
按卦宫五行计算六亲，`SixSpiritRules` 和 `VoidRules` 分别负责六神与旬空。
未确认的完整爻辞数据仍通过 `LineTextRepository` 保持可替换，默认不填充文本。
