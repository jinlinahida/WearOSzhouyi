package com.boompala.engine.pulse

/**
 * 中医脉象调摄资料库与子午流注经络指引。
 * 纯 Kotlin 静态离线资料库，零 I/O 延迟，包含 12 种核心脉象的典籍出处、体征食疗与生活建议。
 */
object PulseCatalog {

    /**
     * 生成标准单周期的经典归一化波形采样点（0f ~ 1f，24个点），供结果卡片绘制典型脉图。
     */
    private fun sampleWaveform(
        peakHeight: Float = 0.95f,
        peakPos: Float = 0.22f,
        notchHeight: Float = 0.40f,
        notchPos: Float = 0.45f,
        dicroticHeight: Float = 0.55f,
        dicroticPos: Float = 0.60f,
    ): List<Float> {
        val points = mutableListOf<Float>()
        val count = 24
        for (i in 0 until count) {
            val t = i.toFloat() / (count - 1)
            val y = when {
                t <= peakPos -> {
                    // 主波升支 (陡峭上升)
                    (t / peakPos) * peakHeight
                }
                t <= notchPos -> {
                    // 主波降支到降中切迹
                    val fraction = (t - peakPos) / (notchPos - peakPos)
                    peakHeight - fraction * (peakHeight - notchHeight)
                }
                t <= dicroticPos -> {
                    // 降中切迹到重搏波峰
                    val fraction = (t - notchPos) / (dicroticPos - notchPos)
                    notchHeight + fraction * (dicroticHeight - notchHeight)
                }
                else -> {
                    // 重搏波衰减回基线
                    val fraction = (t - dicroticPos) / (1f - dicroticPos)
                    dicroticHeight * (1f - fraction)
                }
            }
            points.add(y.coerceIn(0f, 1f))
        }
        return points
    }

    private val PROFILES: Map<PulseCategory, PulseRemedyProfile> = mapOf(
        PulseCategory.PING to PulseRemedyProfile(
            category = PulseCategory.PING,
            featureDescription = "脉位不浮不沉，脉率不快不慢（一息四至至五至），节律规整，从容和缓，应指有力而不紧，气血冲和。",
            waveformPoints = sampleWaveform(peakHeight = 0.88f, notchHeight = 0.38f, dicroticHeight = 0.50f),
            dosList = listOf("顺应四时节律", "保持起居规律", "饮食清淡均衡", "适度舒缓活动"),
            dontsList = listOf("劳逸失度", "暴饮暴食", "昼夜颠倒", "情绪大起大落"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "气血调和（平和体质）",
                    symptoms = "精力充沛，睡眠沉实，食欲正常，面色红润，二便通利。",
                    dietaryRecommendations = "多食五谷杂粮与当季新鲜蔬菜，均衡饮食，维持机体自身阴阳平衡。",
                ),
            ),
            emotionalAdvice = "心平气和，恬淡虚无。保持从容豁达，遇事泰然处之，不作无谓的精神内耗。",
            lifestyleAdvice = "守常规作息，子时（23点）前安卧入眠；晨起推窗吐故纳新；避免长时间久坐不动。",
            exerciseAdvice = "散步、慢跑、太极拳或八段锦，以周身微温微汗为度，保持经络通达畅流。",
            classicLiterature = "《素问·平人气象论》：“人一呼脉再动，一吸脉亦再动，呼吸定息脉五动，闰以太息，命曰平人。平人者，不病也。”",
            theoreticalReason = "平脉为阴阳自和、心气充盛、胃气充盈的外在体现。气血在经脉中顺流不息，故搏动从容不迫。",
        ),
        PulseCategory.HUA to PulseRemedyProfile(
            category = PulseCategory.HUA,
            featureDescription = "往来流利，应指圆滑，如盘走珠。脉体搏动从容充盈，指下如荷叶滚水珠，无任何阻滞涩顿之感。",
            waveformPoints = sampleWaveform(peakHeight = 0.95f, notchHeight = 0.28f, dicroticHeight = 0.65f),
            dosList = listOf("健脾利湿", "消食和中", "微汗排湿", "多饮温热甘淡"),
            dontsList = listOf("肥甘厚味", "生冷冰品", "久坐湿地", "大汗淋漓伤气"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "湿邪阻困（最常见）",
                    symptoms = "伴头重如裹、肢体困倦沉重、午后易困倦嗜睡、舌苔白腻或微厚。",
                    dietaryRecommendations = "宜食山药、冬瓜、赤小豆、茯苓、薏苡仁、陈皮，煮汤代茶，以助脾阳运化水湿。",
                ),
                SyndromeDetail(
                    title = "食积内停",
                    symptoms = "伴脘腹胀满不适、嗳气酸腐、厌恶油腻肉食、大便黏滞不爽。",
                    dietaryRecommendations = "可适量饮用山楂麦芽茶，或食用白萝卜，消积化滞，调畅中焦。",
                ),
                SyndromeDetail(
                    title = "气血盛旺（生理常脉）",
                    symptoms = "青壮年或女性无身体不适，神清气爽，多为气血旺盛之生理反映。",
                    dietaryRecommendations = "保持正常谷肉果菜均衡滋养，不宜过服滋补腻滞之品。",
                ),
            ),
            emotionalAdvice = "舒展胸襟，随遇而安。遇事切忌钻牛角尖或反复纠结，思虑过度极易郁结伤脾，多登高散心疏解气机。",
            lifestyleAdvice = "居处保持干爽通风；避免淋雨涉水或久居潮湿；睡前可用生姜温水泡脚通达下焦水湿；午间小憩蓄养脾阴。",
            exerciseAdvice = "适度快走、慢跑或练习八段锦“调理脾胃须单举”，以周身微温微汗为佳，促水湿随汗孔排解。",
            classicLiterature = "《濒湖脉学》：“滑脉如珠替替疾，往来流利却还期。莫将滑数为同类，数脉浮头滑骨皮。”《素问·脉要精微论》：“滑者，阴气有余也。”",
            theoreticalReason = "脾主运化水湿。若脾失健运，水液代谢迟缓，聚湿成痰流注血脉，使血行流利而质偏浊厚。调摄重在温运中焦、通利水道。",
        ),
        PulseCategory.XIAN to PulseRemedyProfile(
            category = PulseCategory.XIAN,
            featureDescription = "端直以长，如按琴弦。脉管张力偏高，按之挺直而硬，回弹紧实有力，降支切迹抬高，提示机体处于紧绷或气滞状态。",
            waveformPoints = sampleWaveform(peakHeight = 0.92f, notchHeight = 0.60f, dicroticHeight = 0.62f),
            dosList = listOf("疏肝理气", "柔筋缓急", "规律深呼吸", "闭目清神闭目养血"),
            dontsList = listOf("暴躁生闷气", "通宵熬夜", "久坐视屏", "过食酸涩辛燥"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "肝气郁滞（压力偏大）",
                    symptoms = "伴两胁胀满、频频叹气、肩颈僵硬酸痛、入睡困难或梦多不宁。",
                    dietaryRecommendations = "宜饮玫瑰花薄荷茶、佛手茶，多食百合、绿豆、苦瓜、芹菜，疏解肝经郁火。",
                ),
                SyndromeDetail(
                    title = "气滞血瘀",
                    symptoms = "伴局部游走性胀痛或刺痛、头痛紧缩感、唇色微暗。",
                    dietaryRecommendations = "可适量饮用山楂桃仁茶、红糖陈皮水，温通行气活血。",
                ),
            ),
            emotionalAdvice = "放宽心态，宣泄郁结。允许事情不完美，切莫长生闷气，可通过与挚友倾诉、听空灵乐曲放松情绪。",
            lifestyleAdvice = "子丑时（23点至凌晨3点）肝胆经当令，务必进入深层睡眠以滋养肝血；用眼半小时应闭目或远眺绿色。",
            exerciseAdvice = "练习拉伸瑜伽、太极拳圆活运动或柔和慢步，注重拉伸两侧胁肋与腿部肝胆经络，切忌高对抗竞技刺激。",
            classicLiterature = "《濒湖脉学》：“弦脉迢迢端直长，肝经木旺体冲和。春宵满指春和象，病里端直是本色。”《素问·阴阳别论》：“其来如弦，按之虚也。”",
            theoreticalReason = "肝主疏泄而性喜条达。持续精神压力或情绪压抑易引发神经血管紧张度异常增高，脉管收缩如弓弦紧绷。调摄贵在柔肝理气。",
        ),
        PulseCategory.CHI to PulseRemedyProfile(
            category = PulseCategory.CHI,
            featureDescription = "脉率从容偏慢，一息三至（心率通常低于 60 次/分），应指缓慢有余，多主阴盛阳衰或体内虚寒。",
            waveformPoints = sampleWaveform(peakHeight = 0.75f, peakPos = 0.32f, notchHeight = 0.35f, dicroticHeight = 0.45f),
            dosList = listOf("温阳化气", "驱寒固本", "热性饮食", "腰腹下肢防寒"),
            dontsList = listOf("贪凉饮冷", "迎风出汗", "露脐露踝", "过度劳累透支"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "阳虚内寒",
                    symptoms = "伴畏寒肢冷、面色晄白、喜热饮食、精神不振、小便清长。",
                    dietaryRecommendations = "宜食生姜、羊肉、韭菜、红枣、当归、桂圆，烹调加适量肉桂或花椒温中散寒。",
                ),
                SyndromeDetail(
                    title = "寒邪阻络",
                    symptoms = "伴脘腹冷痛、遇冷加剧、得温痛减、手足发凉不温。",
                    dietaryRecommendations = "晨起可饮浓生姜红糖茶，温经通络，驱散体表与经脉凝寒。",
                ),
            ),
            emotionalAdvice = "振奋心神，开朗豁达。避免长时间独处忧郁消极，多与生机盎然的人事相处，沐浴晴朗阳光。",
            lifestyleAdvice = "早卧晚起顺应冬藏；夜间艾灸气海、关元、足三里温煦下元；晨起先喝温热水唤醒脾阳。",
            exerciseAdvice = "慢步快走、传统八段锦前两式，以动生阳，活动至身体发暖微发热即可，切忌大出虚汗受风。",
            classicLiterature = "《脉经》：“迟脉，呼吸三至，去来极迟。”《难经·四难》：“浮者阳也，沉者阴也，迟者阴也，数者阳也。”",
            theoreticalReason = "阳气如天日，主温煦与推动血脉。阳气不足则动力微弱，血流速度减慢；或阴寒阻遏脉道，血流凝滞迟缓。调摄当以温补阳气为要。",
        ),
        PulseCategory.SHU to PulseRemedyProfile(
            category = PulseCategory.SHU,
            featureDescription = "脉来急促，一息六至（心率通常高于 85 次/分），节奏紧凑急促，多主阳盛发热或阴虚内火。",
            waveformPoints = sampleWaveform(peakHeight = 0.95f, peakPos = 0.16f, notchHeight = 0.45f, dicroticHeight = 0.58f),
            dosList = listOf("清热生津", "滋阴降火", "安神静心", "多饮清润津液"),
            dontsList = listOf("辛辣煎炸", "烈酒浓茶", "烈日暴晒", "情绪躁动大怒"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "实热内扰",
                    symptoms = "伴口渴喜冷饮、咽喉干痛、面红目赤、便秘尿赤、烦躁易怒。",
                    dietaryRecommendations = "宜食绿豆、苦瓜、梨、西瓜翠衣、莲子芯、鲜芦根煮水，清热利湿降火。",
                ),
                SyndromeDetail(
                    title = "阴虚内热",
                    symptoms = "伴手足心发热、午后两颧发红、夜间盗汗、咽干口燥、心烦少寐。",
                    dietaryRecommendations = "宜食百合、银耳、麦冬、沙参、枸杞子煲汤，滋养肺胃与肾阴。",
                ),
            ),
            emotionalAdvice = "戒急用忍，平心静气。减少浮躁急切之心，多听清泉流水舒缓音乐，让神识归于清凉宁谧。",
            lifestyleAdvice = "居室保持清爽通风；午时（11点至13点心经当令）务必静卧养阴；睡前半小时不刷激进视频。",
            exerciseAdvice = "静坐调息、柔和伸展，避免在高温燥热环境中运动，运动强度宜低，防止津液大泄。",
            classicLiterature = "《濒湖脉学》：“数脉息间常六至，阴微阳盛必狂烦。浮沉表里分虚实，惟有儿童作吉看。”",
            theoreticalReason = "热性燔灼急迫，气血受火热之邪鼓动，流动急疾。若为实热宜清泻，若为阴虚火旺则需甘寒滋润以养真阴。",
        ),
        PulseCategory.HUAN to PulseRemedyProfile(
            category = PulseCategory.HUAN,
            featureDescription = "一息四至，从容和缓，怠而不急。若应指柔和有力为脾胃健旺之常脉；若怠缓乏力则主中焦湿困脾气虚弱。",
            waveformPoints = sampleWaveform(peakHeight = 0.82f, peakPos = 0.28f, notchHeight = 0.38f, dicroticHeight = 0.48f),
            dosList = listOf("健脾和胃", "通畅气机", "三餐规律", "轻快散步"),
            dontsList = listOf("过饱过饥", "滋腻呆胃", "久坐伤肉", "饮冷伤脾"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "脾湿困阻",
                    symptoms = "伴饮食无味、饭后脘腹微胀、身体怠惰懒动、大便微溏稀软。",
                    dietaryRecommendations = "推荐食用白扁豆、芡实、山药、茯苓莲子粥，健脾化湿利水。",
                ),
            ),
            emotionalAdvice = "放松身心，从容处事。生活节奏宜不疾不徐，凡事留有余地，保持心胸舒坦。",
            lifestyleAdvice = "早饭宜温软易消化；饭后缓行百步以助中焦运化；晚上少吃夜宵以防食滞伤脾。",
            exerciseAdvice = "饭后轻松漫步、散步，八段锦调理脾胃法，轻揉腹部顺时针五十圈。",
            classicLiterature = "《脉经》：“缓脉，小快于迟，一息四至，从容和缓。”《伤寒论》：“阳明脉缓，其人微汗出也。”",
            theoreticalReason = "脾居中焦属土，其性从容和缓。缓而有力为脾胃元气充盈；缓而无力则为脾阳受湿气所困，升降失常。",
        ),
        PulseCategory.RU to PulseRemedyProfile(
            category = PulseCategory.RU,
            featureDescription = "浮而细软，轻触即得，按之稍重则不显，如水上浮絮，轻柔虚无。多主气血两虚兼夹湿气阻滞于表。",
            waveformPoints = sampleWaveform(peakHeight = 0.65f, peakPos = 0.20f, notchHeight = 0.30f, dicroticHeight = 0.38f),
            dosList = listOf("益气健脾", "平补和中", "避湿避风", "温润平补"),
            dontsList = listOf("大热大补", "滋腻碍胃", "重体力透支", "当风纳凉"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "气虚夹湿",
                    symptoms = "伴精神萎靡、神疲无力、说话声音低微、稍微活动即汗出、四肢倦软。",
                    dietaryRecommendations = "宜食党参、白术、茯苓、黄芪小量煮粥，辅以山药、莲子肉，缓补元气。",
                ),
            ),
            emotionalAdvice = "静养心神，淡泊名利。少操心劳力，避免卷入激烈纷争，多听自然鸟鸣水声涵养精力。",
            lifestyleAdvice = "保证充足睡眠，避免熬夜透支；居室防潮除湿；晨起做深长腹式呼吸以固表卫阳。",
            exerciseAdvice = "温和漫步或站桩五至十分钟，以身体发热不喘微有活力为限，切不可勉强剧烈运动。",
            classicLiterature = "《濒湖脉学》：“濡形浮细按之无，月映微云气力微。主病虚寒与湿气，阳微骨蒸此中得。”",
            theoreticalReason = "濡脉乃正气亏虚不能充实体表，且有水湿之邪阻遏经隧，使得脉浮无根而细软如绵。调养须扶正兼化湿浊。",
        ),
        PulseCategory.XI to PulseRemedyProfile(
            category = PulseCategory.XI,
            featureDescription = "脉管狭细，按之细如丝线，但应指轮廓分明，搏动绵绵不绝。多主阴血精津严重匮乏，脉道失养。",
            waveformPoints = sampleWaveform(peakHeight = 0.60f, peakPos = 0.25f, notchHeight = 0.22f, dicroticHeight = 0.30f),
            dosList = listOf("补养气血", "养阴生津", "安静休养", "食补温润"),
            dontsList = listOf("大汗伤津", "过度用眼", "熬夜损阴", "盲目节食"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "气血两虚",
                    symptoms = "伴头昏目眩、面色萎黄失华、失眠多梦健忘、爪甲苍白无血色。",
                    dietaryRecommendations = "宜食桂圆、桑椹、黑芝麻、红枣、当归黄芪乌鸡汤，温和滋补营血脉道。",
                ),
            ),
            emotionalAdvice = "定心凝神，避免悲伤忧郁。遇事不必苛责自己，减少无谓的心力消耗与情感消耗。",
            lifestyleAdvice = "肝开窍于目，久视伤血，须严限屏幕使用时间；午间及晚间按时卧床休养，养足气血。",
            exerciseAdvice = "慢走、轻柔瑜伽拉伸或传统站桩，重在通达微循环，切忌剧烈消耗体力的无氧运动。",
            classicLiterature = "《脉经》：“细脉，小大于微，常有，但细耳。”《濒湖脉学》：“细来累累细如丝，应指分明不甚迟。”",
            theoreticalReason = "营血亏损不能充实于脉，脉管空瘪狭小；气虚无力充沛鼓动，故脉细如丝。调摄当以培补气血、润养脏腑为本。",
        ),
        PulseCategory.CHEN to PulseRemedyProfile(
            category = PulseCategory.CHEN,
            featureDescription = "脉位深潜，轻取皮表无所觉，重按至筋骨方得搏动，沉伏有力或无力，主里寒水气或气机内收。",
            waveformPoints = sampleWaveform(peakHeight = 0.70f, peakPos = 0.35f, notchHeight = 0.32f, dicroticHeight = 0.40f),
            dosList = listOf("温通里阳", "固护下元", "腰膝保暖", "内观蓄精"),
            dontsList = listOf("受寒受风", "大汗外越", "冷水洗澡", "过用寒凉下药"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "里寒水气",
                    symptoms = "伴形寒畏冷、腹胀便溏、腰膝酸重冷痛、下肢轻度发紧或水湿内停。",
                    dietaryRecommendations = "宜食羊肉、干姜、小茴香、茯苓、胡椒，温阳化气行水。",
                ),
            ),
            emotionalAdvice = "深沉内省，冷静从容。遇变不慌不躁，多反思内求，减少浮躁张扬。",
            lifestyleAdvice = "晚间艾灸神阙（肚脐）、命门、涌泉穴；注意下肢保暖避寒；睡前泡脚助阳回生。",
            exerciseAdvice = "站桩功、少林易筋经引气下行，重点强壮腰膝，以促真阳内固。",
            classicLiterature = "《脉经》：“沉脉，按之有余，举之无有。”《素问·玉机真脏论》：“冬脉者，万物闭藏，故其气来沉以搏。”",
            theoreticalReason = "冬令闭藏或真阳亏虚，气血内敛深藏不能充发于外，脉道深伏近骨。调养宜温补命门之火，导气归元。",
        ),
        PulseCategory.FU to PulseRemedyProfile(
            category = PulseCategory.FU,
            featureDescription = "脉位浅表，举手轻触皮表即明显搏动，重按则力量稍减但不空，如木浮水面，多主外感风邪或表卫失调。",
            waveformPoints = sampleWaveform(peakHeight = 0.90f, peakPos = 0.18f, notchHeight = 0.40f, dicroticHeight = 0.52f),
            dosList = listOf("调和营卫", "疏风解表", "适量饮热汤", "避风固表"),
            dontsList = listOf("风口纳凉", "冷热交替受风", "强行冰敷", "油腻阻表"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "外感表邪（初起常见）",
                    symptoms = "伴头痛项强、鼻塞流涕、恶风怕冷或身热微渴。",
                    dietaryRecommendations = "风寒宜食葱白生姜红糖汤发汗；风热宜食金银花薄荷菊花茶清凉解表。",
                ),
            ),
            emotionalAdvice = "平抑心浮气躁，勿躁勿恼。静心安神，少言语耗气，避开外界嘈杂纷乱扰动。",
            lifestyleAdvice = "外出注意颈部保暖防风；出汗后切勿立即吹风或洗冷水；多喝温开水促邪从表解。",
            exerciseAdvice = "表邪在体宜室内安静静卧修养，暂停一切耗气之剧烈运动。",
            classicLiterature = "《脉经》：“浮脉，举之有余，按之不足。”《濒湖脉学》：“浮脉惟从肉上行，如循榆荚似飞轻。”",
            theoreticalReason = "正气抗邪于体表肌腠，阳气奋发向外，气血集中浮盛于外周脉管。调养应顺其气势，助正气驱邪外出。",
        ),
        PulseCategory.HONG to PulseRemedyProfile(
            category = PulseCategory.HONG,
            featureDescription = "来盛去衰，滔滔满指。脉体极大极宽，搏动汹涌澎湃，来时气势磅礴，去时力渐回落，多主阳明热盛极盛。",
            waveformPoints = sampleWaveform(peakHeight = 1.00f, peakPos = 0.15f, notchHeight = 0.50f, dicroticHeight = 0.68f),
            dosList = listOf("清泻胃火", "生津止渴", "多食寒凉甘润", "居室避温保凉"),
            dontsList = listOf("温燥大热", "饮酒辛辣", "烈日劳作", "烦躁发怒"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "阳明经实热",
                    symptoms = "伴大热、大汗、大渴引饮、面赤咽干、心烦失眠、大便燥结。",
                    dietaryRecommendations = "宜食西瓜、鲜芦根、竹叶绿豆汤、麦冬、百合，大甘大寒以清热养阴。",
                ),
            ),
            emotionalAdvice = "退步省心，熄灭怒火。克制冲动急躁，以清凉退火为先，静待心火沉降。",
            lifestyleAdvice = "居室宜清凉安静避光；穿着宽松轻薄透气棉麻；午后小啜凉白开滋润津液。",
            exerciseAdvice = "完全静息，切忌任何剧烈运动以防加重热势与脱水耗津。",
            classicLiterature = "《脉经》：“洪脉，极大在指下。”《濒湖脉学》：“洪脉滔滔势自然，来时极大去多衰。”",
            theoreticalReason = "内热充斥内外，鼓动气血如江河翻滚，血管极度舒张。调摄当以甘寒之品折其狂火，生津止渴。",
        ),
        PulseCategory.JIE_DAI to PulseRemedyProfile(
            category = PulseCategory.JIE_DAI,
            featureDescription = "脉律偶止，或迟而时止，或止有常数（节律规整度较低，伴漏搏或间期跳跃），多主心气心阳亏虚或痰瘀阻滞。",
            waveformPoints = sampleWaveform(peakHeight = 0.70f, peakPos = 0.26f, notchHeight = 0.35f, dicroticHeight = 0.42f),
            dosList = listOf("养心安神", "益气复脉", "安心静卧", "心境祥和"),
            dontsList = listOf("惊恐过劳", "咖啡浓茶", "熬夜失眠", "剧烈屏气运动"),
            syndromes = listOf(
                SyndromeDetail(
                    title = "心气心血不足",
                    symptoms = "伴心悸怔忡、心慌胸闷、短气乏力、睡眠浅易惊醒。",
                    dietaryRecommendations = "宜食酸枣仁、桂圆、莲子肉、远志、黄芪枸杞红枣茶，补益心气滋养心血。",
                ),
            ),
            emotionalAdvice = "安心守志，远离惊恐。切莫悲观焦虑，减少刺激性视听，保持平和温厚心态。",
            lifestyleAdvice = "规律作息早卧；避开嘈杂环境；睡前温和揉按内关穴、神门穴各五十次以宁心安神。",
            exerciseAdvice = "以静养为主，可平地缓慢散步二十分钟，切不可做任何负重跑步或憋气运动。",
            classicLiterature = "《伤寒论》：“脉结代，心动悸，炙甘草汤主之。”《濒湖脉学》：“结脉缓而时一止，代脉止有常数。”",
            theoreticalReason = "心主血脉。若心气心阳亏虚，或痰浊瘀血阻滞心脉，气血流通受阻不能相续，故脉行歇止。调养须培补心气、活血复脉。",
        ),
    )

    /**
     * 获取指定脉象的完整调摄与典籍指引。
     */
    fun getProfile(category: PulseCategory): PulseRemedyProfile {
        return PROFILES[category] ?: PROFILES.getValue(PulseCategory.PING)
    }

    /**
     * 获取全部 12 种脉象字典。
     */
    fun allProfiles(): Map<PulseCategory, PulseRemedyProfile> = PROFILES

    /**
     * 根据 24 小时制设备时间推算子午流注十二经络时辰信息。
     */
    fun getMeridianInfluence(hour24: Int): MeridianInfluence {
        return when (hour24) {
            23, 0 -> MeridianInfluence(
                earthlyBranch = "子时",
                timeRangeText = "23:00 - 01:00",
                meridianName = "足少阳胆经",
                organName = "胆",
                physiologicalRole = "胆经当令 · 阳气初生",
                healthGuidance = "子时胆经气血最旺，一阳初动，宜静卧安睡以蓄养胆气。",
            )
            1, 2 -> MeridianInfluence(
                earthlyBranch = "丑时",
                timeRangeText = "01:00 - 03:00",
                meridianName = "足厥阴肝经",
                organName = "肝",
                physiologicalRole = "肝经当令 · 肝血归藏",
                healthGuidance = "丑时人卧血归于肝，深层睡眠有助于肝脏排毒与血精滋养。",
            )
            3, 4 -> MeridianInfluence(
                earthlyBranch = "寅时",
                timeRangeText = "03:00 - 05:00",
                meridianName = "手太阴肺经",
                organName = "肺",
                physiologicalRole = "肺经当令 · 气血朝百脉",
                healthGuidance = "寅时肺经司朝百脉，输布气血至周身，熟睡有助于调和呼吸。",
            )
            5, 6 -> MeridianInfluence(
                earthlyBranch = "卯时",
                timeRangeText = "05:00 - 07:00",
                meridianName = "手阳明大肠经",
                organName = "大肠",
                physiologicalRole = "大肠经当令 · 传导糟粕",
                healthGuidance = "天门开地户开，宜晨起饮温水一杯，润肠通便排泄湿热浊物。",
            )
            7, 8 -> MeridianInfluence(
                earthlyBranch = "辰时",
                timeRangeText = "07:00 - 09:00",
                meridianName = "足阳明胃经",
                organName = "胃",
                physiologicalRole = "胃经当令 · 受纳水谷",
                healthGuidance = "辰时天地阳气充盛，胃气最旺，务必享用温热易消化之丰盛早餐。",
            )
            9, 10 -> MeridianInfluence(
                earthlyBranch = "巳时",
                timeRangeText = "09:00 - 11:00",
                meridianName = "足太阴脾经",
                organName = "脾",
                physiologicalRole = "脾经当令 · 运化水谷精微",
                healthGuidance = "巳时脾主运化，大脑思维活跃敏捷，宜专心投入工作与学习。",
            )
            11, 12 -> MeridianInfluence(
                earthlyBranch = "午时",
                timeRangeText = "11:00 - 13:00",
                meridianName = "手少阴心经",
                organName = "心",
                physiologicalRole = "心经当令 · 君主之官主神明",
                healthGuidance = "一阴初生之时，宜午间闭目小憩一刻钟，以平衡心火蓄养精神。",
            )
            13, 14 -> MeridianInfluence(
                earthlyBranch = "未时",
                timeRangeText = "13:00 - 15:00",
                meridianName = "手太阳小肠经",
                organName = "小肠",
                physiologicalRole = "小肠经当令 · 分清泌浊",
                healthGuidance = "小肠汲取水谷精华，适度多补充温水，协助吸收津液与气化。",
            )
            15, 16 -> MeridianInfluence(
                earthlyBranch = "申时",
                timeRangeText = "15:00 - 17:00",
                meridianName = "足太阳膀胱经",
                organName = "膀胱",
                physiologicalRole = "膀胱经当令 · 水津气化出表",
                healthGuidance = "太阳主通调水道，体能精力处于黄金期，适度饮水、运动与排便。",
            )
            17, 18 -> MeridianInfluence(
                earthlyBranch = "酉时",
                timeRangeText = "17:00 - 19:00",
                meridianName = "足少阴肾经",
                organName = "肾",
                physiologicalRole = "肾经当令 · 封藏气血元精",
                healthGuidance = "日落归息，肾主藏精，晚餐宜清淡适量，不宜过度繁劳伤及下元。",
            )
            19, 20 -> MeridianInfluence(
                earthlyBranch = "戌时",
                timeRangeText = "19:00 - 21:00",
                meridianName = "手厥阴心包经",
                organName = "心包",
                physiologicalRole = "心包经当令 · 护卫心神代君受过",
                healthGuidance = "心包受卫，宜放松心情、散步散心、听柔和音乐，莫谈郁闷烦心事。",
            )
            21, 22 -> MeridianInfluence(
                earthlyBranch = "亥时",
                timeRangeText = "21:00 - 23:00",
                meridianName = "手少阳三焦经",
                organName = "三焦",
                physiologicalRole = "三焦经当令 · 调畅周身水道气机",
                healthGuidance = "三焦主通行水谷诸阳，入夜阴气渐重，宜温水泡脚，静候安然入眠。",
            )
            else -> MeridianInfluence(
                earthlyBranch = "午时",
                timeRangeText = "11:00 - 13:00",
                meridianName = "手少阴心经",
                organName = "心",
                physiologicalRole = "心经当令 · 神明内守",
                healthGuidance = "午后宜小憩片刻以养心阴。",
            )
        }
    }
}
