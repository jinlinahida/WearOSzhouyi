# 六爻爻辞数据来源与再分发评估

调研日期：2026-07-30
目的：为 `engine/src/main/assets/yao_text.json` 选择可离线打包、可追溯且允许再分发的 384 条标准爻辞来源。

本文只记录来源和许可结论；爻辞 JSON 应在实现阶段按本文的固定版本和归属要求生成。本文不复制爻辞正文。

## 推荐来源：中文维基文库《周易》

主要来源：

- [《周易》总页](https://zh.wikisource.org/wiki/周易)
- [固定版本的总页（oldid=7907208）](https://zh.wikisource.org/w/index.php?title=周易&oldid=7907208)
- [乾卦页（oldid=2404991）](https://zh.wikisource.org/w/index.php?title=周易/乾&oldid=2404991)
- [坤卦页（oldid=2404999）](https://zh.wikisource.org/w/index.php?title=周易/坤&oldid=2404999)
- [未济卦页（oldid=2405020）](https://zh.wikisource.org/w/index.php?title=周易/未濟&oldid=2405020)
- [MediaWiki API 查询入口](https://zh.wikisource.org/w/api.php)

总页将《周易》描述为六十四卦、三百八十四爻，并在页面底部明确标注该作品为全球公有领域。各卦页分别列出卦辞、六条标准爻辞及《象》《彖》等传文；乾卦和坤卦另有“用九”“用六”，它们不是六个位置爻之一。

### 许可判断

这里需要区分“古代原典”和“网站转录”：

1. 《周易》原典是古代作品。维基文库总页将该作品标记为公有领域，因此原典文字本身不应再产生现代作者版权。
2. 维基文库的[版权政策](https://wikisource.org/wiki/Wikisource:Copyright_policy)规定，作品必须是公有领域作品或以 CC BY-SA 4.0 提供；其“Redistribution”部分允许对公有领域材料自由使用、修改和再分发。
3. 卦页包含现代数字转录、标点和编辑排版。为避免对“原典无版权”和“转录编辑贡献”作过度推断，本项目对随应用发布的结构化转录采取更保守的做法：将 `yao_text.json` 作为来源转录的 CC BY-SA 4.0 数据，保留来源、版本、修改说明，并将修改后的数据文件继续以 CC BY-SA 4.0 提供。
4. [CC BY-SA 4.0 条款摘要](https://creativecommons.org/licenses/by-sa/4.0/)明确允许商业用途的复制、再分发和改编，但要求署名、附许可证链接、说明修改，并对改编后的同一数据材料继续采用相同许可证。该数据许可应与应用源码、算法代码和 UI 代码分开说明；不应把 CC BY-SA 自动声称为整个 APK 的源码许可证。

建议在 `yao_text.json` 的元数据或同目录 `NOTICE-yao-text.txt` 中保留：

- 来源名称：中文维基文库《周易》
- 访问日期
- 每个卦页的固定 `oldid`（或一个可复现的 revision manifest）
- 来源页面链接
- `CC BY-SA 4.0` 许可证链接
- “已去除 MediaWiki 展示标记和版式脚注，并重排为 64×6 JSON；未加入译文或解释”的修改说明

如果未来必须采用 MIT/Apache/CC0 这类无 ShareAlike 的数据许可证，应另行从已确认的公版影印本逐条核对、独立转录并由项目明确声明自己的数据贡献；不能把未知 GitHub 仓库的 MIT 许可证当作其内含古籍文本的授权。

## 384 条覆盖核验

2026-07-30 通过 MediaWiki API 获取 `周易/` 前缀页面，并固定查询结果中的 revision ID。查询使用 `generator=allpages`、`gapprefix=周易/`、`prop=revisions`、`rvprop=ids|content`、`rvslots=main`；API 返回 continuation 时继续请求 `rvcontinue`。

核验结果：

| 项目 | 数量 |
| --- | ---: |
| 六十四卦正文页 | 64 |
| 卦页中带编号的《易經》文本行 | 386 |
| 乾卦“用九” | 1 |
| 坤卦“用六” | 1 |
| 标准位置爻辞（386 - 2） | **384** |
| 每个卦页的标准位置行数 | 6 |

计数时只保留每个卦页《易經》部分的六个位置：初爻、二爻、三爻、四爻、五爻、上爻；“用九”“用六”不映射到 `YaoPosition`，因此不放入 384 条位置数据。卦页中的《象》《彖》《文言》等传文也不计入爻辞。

### 版本固定要求

维基文库页面会继续编辑。实现时不能只记录 `https://zh.wikisource.org/wiki/周易/乾` 这类浮动链接；应保存每个卦页的 `oldid`，或者把 API 返回的 64 个页面 revision ID 写入数据清单。这样可以：

- 复现当前 `yao_text.json` 的精确文字版本；
- 发现来源页面后续出现的异体字、标点或校订变化；
- 在数据更新时生成可审查的 diff，而不是静默改变排盘结果。

## 文本规范化注意事项

维基文库页面使用繁体字，并可能包含 MediaWiki 的字词转换标记（例如 `-{...}-`）。生成 JSON 时只能去除标记语法并保留其明确文本，不应把《易经》文字自动简繁转换、翻译或加入现代解释。不同来源可能存在 `无/無`、`羣/群`、标点和个别异文差异；本项目应选择一个固定版本，避免在同一数据集内混用维基文库和其他版本。

建议的 JSON 结构为“卦唯一编码 + 位置”索引，而不是按 King Wen 序号隐式索引，例如：

```json
{
  "schemaVersion": 1,
  "source": {
    "name": "中文维基文库《周易》",
    "license": "CC BY-SA 4.0",
    "licenseUrl": "https://creativecommons.org/licenses/by-sa/4.0/",
    "retrievedAt": "2026-07-30",
    "revisionManifest": "..."
  },
  "lines": [
    {
      "hexagramCodeFromBottom": "111111",
      "position": 1,
      "text": "..."
    }
  ]
}
```

`text` 应只保存爻辞正文；爻题（如“初九”）由 `position` 和排盘阴阳状态生成，避免把固定的卦爻阴阳标题误当作运行时输入。

## 排除的来源

### Project Gutenberg eBook #25501《易經》

[项目页](https://www.gutenberg.org/ebooks/25501)及[纯文本](https://www.gutenberg.org/ebooks/25501.txt.utf-8)包含完整《易經》正文，适合用作人工交叉校对。它不是本项目的首选离线数据源，原因是：

- 项目页明确写的是“Public domain in the USA”，并要求美国以外的使用者自行核对当地法律；
- 电子书附有 Project Gutenberg License。该许可允许转换和再分发，但在保留 Project Gutenberg 标识时要求保留特定许可声明和即时访问许可全文；其商标条款会给应用分发增加额外合规负担；
- 其版本的异体字、标点和个别字形与维基文库版本不同，不应未经校勘直接混入同一 JSON。

因此，Gutenberg 版本只作为人工抽查参考，不作为 `yao_text.json` 的授权主来源。

### Chinese Text Project（中国哲学书电子化计划）

[FAQ 的版权说明](https://ctext.org/faq)明确说明网站及内容受国际版权保护，不能未经书面许可再发布；其公开许可仅覆盖有限的个人、非营利学术使用和合理引用，并且禁止自动化批量下载。即使其底本本身是公版，也不能把站点转录直接打包进 APK。因此不使用其页面或 API 作为数据来源。

### 未确认数据来源的 GitHub “MIT” 项目

仓库根目录的 MIT/Apache/BSD 许可证只证明作者对其代码（以及明确标注为该许可证的数据）授予相应权限，不能自动证明仓库内古籍文本的版权链、转录版本或再分发权。调研中看到的六爻项目还存在“爻辞部分待补充”、没有数据文件级许可或没有完整 384 条的情况，因此不直接复制其文本。

## 当前结论

推荐实现顺序：

1. 以中文维基文库固定 revision 的 64 个卦页为唯一主来源；
2. 生成 64×6 的 `yao_text.json`，排除“用九”“用六”；
3. 在 assets 同目录附来源与 CC BY-SA 4.0 说明；
4. 用独立校验脚本或单元测试断言条目数为 384、每卦恰好 6 条，并抽查乾、坤、未济等固定条目；
5. 后续若发现版本异文，提交新的 revision manifest 和数据变更说明，不静默覆盖。
