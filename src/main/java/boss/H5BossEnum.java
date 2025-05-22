package boss;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
public class H5BossEnum {
    @Getter
    public enum Experience {
        NULL("不限", "0"),
        STUDENT("在校生", "e_108"),
        GRADUATE("应届毕业生", "e_102"),
        LESS_THAN_ONE_YEAR("1年以下", "e_103"),
        ONE_TO_THREE_YEARS("1-3年", "e_104"),
        THREE_TO_FIVE_YEARS("3-5年", "e_105"),
        FIVE_TO_TEN_YEARS("5-10年", "e_106"),
        MORE_THAN_TEN_YEARS("10年以上", "e_107");

        private final String name;
        private final String code;

        Experience(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public static Optional<String> getCode(String name) {
            return Arrays.stream(Experience.values()).filter(experience -> experience.name.equals(name)).findFirst().map(experience -> experience.code);
        }

        @JsonCreator
        public static Experience forValue(String value) {
            for (Experience experience : Experience.values()) {
                if (experience.name.equals(value)) {
                    return experience;
                }
            }
            return NULL;
        }
    }

    @Getter
    public enum CityCode {
        NULL("不限", "c0"),
        ALL("全国", "c100010000"),
        ANSHAN("鞍山", "c101070300"),
        ALASHAN_MENG("阿拉善盟", "c101081200"),
        ANKANG("安康", "c101110700"),
        AKESU("阿克苏地区", "c101131000"),
        ALETAI_DIQU("阿勒泰地区", "c101131500"),
        ALAER("阿拉尔", "c101131700"),
        ALI_DIQU("阿里地区", "c101140700"),
        ANYANG("安阳", "c101180200"),
        ANQING("安庆", "c101220600"),
        ANSHUN("安顺", "c101260300"),
        ABA("阿坝藏族羌族自治州", "c101271900"),
        MACAU("澳门", "c101330100"),
        BEIJING("北京", "c101010100"),
        BAICHENG("白城", "c101060500"),
        BAISHAN("白山", "c101060800"),
        BAOTOU("包头", "c101080200"),
        BAYAN_NAOER("巴彦淖尔", "c101080800"),
        BAODING("保定", "c101090200"),
        BAOJI("宝鸡", "c101110900"),
        BINZHOU("滨州", "c101121100"),
        BAYINGUOLENG("巴音郭楞蒙古自治州", "c101130400"),
        BOERTALA("博尔塔拉蒙古自治州", "c101130500"),
        BEITUN("北屯市", "c101132100"),
        BAIYANG("白杨市", "c101132700"),
        BAIYIN("白银", "c101161000"),
        BENGBU("蚌埠", "c101220200"),
        BOZHOU("亳州", "c101220900"),
        BIJIE("毕节", "c101260500"),
        BAZHONG("巴中", "c101270900"),
        BAOSHAN("保山", "c101290300"),
        BAISE("百色", "c101301000"),
        BEIHAI("北海", "c101301300"),
        BAISHA("白沙黎族自治县", "c101311400"),
        BAOTING("保亭黎族苗族自治县", "c101311800"),
        CHONGQING("重庆", "c101040100"),
        CHANGCHUN("长春", "c101060100"),
        ZHAOYANG("朝阳", "c101071200"),
        CHIFENG("赤峰", "c101080500"),
        CHENGDE("承德", "c101090400"),
        CANGZHOU("沧州", "c101090700"),
        CHANGZHI("长治", "c101100500"),
        CHANGJI("昌吉回族自治州", "c101130300"),
        CHANGDU("昌都", "c101140300"),
        CHANGZHOU("常州", "c101191100"),
        CHUZHOU("滁州", "c101221000"),
        CHIZHOU("池州", "c101221500"),
        CHANGSHA("长沙", "c101250100"),
        CHENZHOU("郴州", "c101250500"),
        CHANGDE("常德", "c101250600"),
        CHENGDU("成都", "c101270100"),
        CHAOZHOU("潮州", "c101281500"),
        CHUXIONG("楚雄彝族自治州", "c101291700"),
        CHONGZUO("崇左", "c101300200"),
        CHENGMAI("澄迈", "c101311200"),
        CHANGJIANG("昌江黎族自治县", "c101311500"),
        DAQING("大庆", "c101050800"),
        DAXING_ANLING("大兴安岭地区", "c101051300"),
        DALIAN("大连", "c101070200"),
        DANDONG("丹东", "c101070600"),
        DATONG("大同", "c101100200"),
        DEZHOU("德州", "c101120400"),
        DONGYING("东营", "c101121200"),
        DINGXI("定西", "c101160200"),
        DAZHOU("达州", "c101270600"),
        DEYANG("德阳", "c101271700"),
        DONGGUAN("东莞", "c101281600"),
        DONGSHA("东沙群岛", "c101282200"),
        DEHONG("德宏傣族景颇族自治州", "c101291300"),
        DIQING("迪庆藏族自治州", "c101291500"),
        DALIBAI("大理白族自治州", "c101291600"),
        DANZHOU("儋州", "c101310400"),
        DONGFANG("东方", "c101310900"),
        DINGAN("定安", "c101311000"),
        EERDUOSI("鄂尔多斯", "c101080600"),
        EZHOU("鄂州", "c101200300"),
        ENSHI("恩施土家族苗族自治州", "c101201300"),
        FUSHUN("抚顺", "c101070400"),
        FUXIN("阜新", "c101070900"),
        FUYANG("阜阳", "c101220800"),
        FUZHOU("福州", "c101230100"),
        FUZHOU2("抚州", "c101240400"),
        FOSHAN("佛山", "c101280800"),
        FANGCHENGGANG("防城港", "c101301400"),
        GUOLUO("果洛藏族自治州", "c101150600"),
        GANNAN("甘南藏族自治州", "c101161400"),
        GUYUAN("固原", "c101170400"),
        GANZHOU("赣州", "c101240700"),
        GUIYANG("贵阳", "c101260100"),
        GUANGAN("广安", "c101270800"),
        GUANGYUAN("广元", "c101271800"),
        GANZI("甘孜藏族自治州", "c101272100"),
        GUANGZHOU("广州", "c101280100"),
        GUILIN("桂林", "c101300500"),
        GUIGANG("贵港", "c101300800"),
        HAERBIN("哈尔滨", "c101050100"),
        HEIHE("黑河", "c101050600"),
        HEGANG("鹤岗", "c101051100"),
        HULUDAO("葫芦岛", "c101071400"),
        HUHEHAOTE("呼和浩特", "c101080100"),
        HULUNBEIER("呼伦贝尔", "c101080700"),
        HENGSHUI("衡水", "c101090800"),
        HANDAN("邯郸", "c101091000"),
        HANZHONG("汉中", "c101110800"),
        HEZE("菏泽", "c101121000"),
        HAMI("哈密", "c101130900"),
        HETIAN("和田地区", "c101131300"),
        HUANGHE("胡杨河市", "c101132600"),
        HAIDONG("海东", "c101150200"),
        HAIBEI("海北藏族自治州", "c101150300"),
        HUANGNAN("黄南藏族自治州", "c101150400"),
        HAINAN("海南藏族自治州", "c101150500"),
        HAIXI("海西蒙古族藏族自治州", "c101150800"),
        HEBI("鹤壁", "c101181200"),
        HUAIAN("淮安", "c101190900"),
        HUANGGANG("黄冈", "c101200500"),
        HUANGSHI("黄石", "c101200600"),
        HANGZHOU("杭州", "c101210100"),
        HUZHOU("湖州", "c101210200"),
        HEFEI("合肥", "c101220100"),
        HUAINAN("淮南", "c101220400"),
        HUAIBEI("淮北", "c101221100"),
        HUANGSHAN("黄山", "c101221600"),
        HENGYANG("衡阳", "c101250400"),
        HUAIHUA("怀化", "c101251200"),
        HUIZHOU("惠州", "c101280300"),
        HEYUAN("河源", "c101281200"),
        HONGHE("红河哈尼族彝族自治州", "c101291200"),
        HEZHOU("贺州", "c101300700"),
        HECHI("河池", "c101301200"),
        HAIKOU("海口", "c101310100"),
        JIAMUSI("佳木斯", "c101050400"),
        JIXI("鸡西", "c101051000"),
        JILIN("吉林", "c101060200"),
        JINZHOU("锦州", "c101070700"),
        JINZHONG("晋中", "c101100400"),
        JINCHENG("晋城", "c101100600"),
        JINAN("济南", "c101120100"),
        JINING("济宁", "c101120700"),
        JINCHANG("金昌", "c101160600"),
        JIUQUAN("酒泉", "c101160800"),
        JIAYUGUAN("嘉峪关", "c101161200"),
        JIAOZUO("焦作", "c101181100"),
        JIYUAN("济源", "c101181800"),
        JINGZHOU("荆州", "c101200800"),
        JINGMEN("荆门", "c101201200"),
        JIAXING("嘉兴", "c101210300"),
        JINHUA("金华", "c101210900"),
        JIUJIANG("九江", "c101240200"),
        JIAN("吉安", "c101240600"),
        JINGDEZHEN("景德镇", "c101240800"),
        JIANGMEN("江门", "c101281100"),
        JIEYANG("揭阳", "c101281900"),
        KELAMAYI("克拉玛依", "c101130200"),
        KEZILESU("克孜勒苏柯尔克孜自治州", "c101131100"),
        KASHI("喀什地区", "c101131200"),
        KEKEDALA("可克达拉市", "c101132200"),
        KUNYU("昆玉市", "c101132300"),
        KAIFENG("开封", "c101180800"),
        KUNMING("昆明", "c101290100"),
        LIAOYUAN("辽源", "c101060600"),
        LIAOYANG("辽阳", "c101071000"),
        LANGFANG("廊坊", "c101090600"),
        LINFEN("临汾", "c101100700"),
        LVLIANG("吕梁", "c101101100"),
        LINYI("临沂", "c101120900"),
        LIAOCHENG("聊城", "c101121700"),
        LHASA("拉萨", "c101140100"),
        LINZHI("林芝", "c101140400"),
        LANZHOU("兰州", "c101160100"),
        LONGNAN("陇南", "c101161100"),
        LINXIA("临夏回族自治州", "c101161300"),
        LUOYANG("洛阳", "c101180900"),
        LUOHE("漯河", "c101181500"),
        LIANYUNGANG("连云港", "c101191000"),
        LI_SHUI("丽水", "c101210800"),
        LIUAN("六安", "c101221400"),
        LONGYAN("龙岩", "c101230700"),
        LOUDI("娄底", "c101250800"),
        LIUPANSHUI("六盘水", "c101260600"),
        LUZHOU("泸州", "c101271000"),
        LESHAN("乐山", "c101271400"),
        LIANGSHAN("凉山彝族自治州", "c101272000"),
        LIN_CANG("临沧", "c101290800"),
        LIJIANG("丽江", "c101290900"),
        LIUZHOU("柳州", "c101300300"),
        LAIBIN("来宾", "c101300400"),
        LINGAO("临高", "c101311300"),
        LEDONG("乐东黎族自治县", "c101311600"),
        LINGSHUI("陵水黎族自治县", "c101311700"),
        MUDANJIANG("牡丹江", "c101050300"),
        MAANSHAN("马鞍山", "c101220500"),
        MIANYANG("绵阳", "c101270400"),
        MEISHAN("眉山", "c101271500"),
        MEIZHOU("梅州", "c101280400"),
        MAOMING("茂名", "c101282000"),
        NAQU("那曲", "c101140600"),
        NANYANG("南阳", "c101180700"),
        NANJING("南京", "c101190100"),
        NANTONG("南通", "c101190500"),
        NINGBO("宁波", "c101210400"),
        NINGDE("宁德", "c101230300"),
        NANPING("南平", "c101230900"),
        NANCHANG("南昌", "c101240100"),
        NANCHONG("南充", "c101270500"),
        NEIJIANG("内江", "c101271200"),
        NUJIANG("怒江傈僳族自治州", "c101291400"),
        NANNING("南宁", "c101300100"),
        PANJIN("盘锦", "c101071300"),
        PINGLIANG("平凉", "c101160300"),
        PINGDINGSHAN("平顶山", "c101180500"),
        PUYANG("濮阳", "c101181300"),
        PUTIAN("莆田", "c101230400"),
        PINGXIANG("萍乡", "c101240900"),
        PANZHIHUA("攀枝花", "c101270200"),
        PUER("普洱", "c101290500"),
        QIQIHAER("齐齐哈尔", "c101050200"),
        QITAIHE("七台河", "c101050900"),
        QINHUANGDAO("秦皇岛", "c101091100"),
        QINGDAO("青岛", "c101120200"),
        QINGYANG("庆阳", "c101160400"),
        QIANJIANG("潜江", "c101201500"),
        QUZHOU("衢州", "c101211000"),
        QUANZHOU("泉州", "c101230500"),
        QIANDONGNAN("黔东南苗族侗族自治州", "c101260700"),
        QIANNAN("黔南布依族苗族自治州", "c101260800"),
        QIANXINAN("黔西南布依族苗族自治州", "c101260900"),
        QINGYUAN("清远", "c101281300"),
        QUJING("曲靖", "c101290200"),
        QINZHOU("钦州", "c101301100"),
        QIONGHAI("琼海", "c101310600"),
        QIONGZHONG("琼中黎族苗族自治县", "c101311900"),
        RIZHAO("日照", "c101121500"),
        RIKAZE("日喀则", "c101140200"),
        SHANGHAI("上海", "c101020100"),
        SUIHUA("绥化", "c101050500"),
        SHUANGYASHAN("双鸭山", "c101051200"),
        SIPING("四平", "c101060300"),
        SONGYUAN("松原", "c101060700"),
        SHENYANG("沈阳", "c101070100"),
        SHIJIAZHUANG("石家庄", "c101090100"),
        SHUOZHOU("朔州", "c101100900"),
        SHANGLUO("商洛", "c101110600"),
        SHIHEZI("石河子", "c101131600"),
        SHUANGHESHI("双河市", "c101132400"),
        SHANNAN("山南", "c101140500"),
        SHIZUISHAN("石嘴山", "c101170200"),
        SHANGQIU("商丘", "c101181000"),
        SANMENXIA("三门峡", "c101181700"),
        SUZHOU("苏州", "c101190400"),
        SUQIAN("宿迁", "c101191300"),
        SHIYAN("十堰", "c101201000"),
        SUIZHOU("随州", "c101201100"),
        SHENNONGJIA("神农架", "c101201700"),
        SHAOXING("绍兴", "c101210500"),
        SUZHOU2("宿州", "c101220700"),
        SANMING("三明", "c101230800"),
        SHANGRAO("上饶", "c101240300"),
        SHAOYANG("邵阳", "c101250900"),
        SUINING("遂宁", "c101270700"),
        SHAOGUAN("韶关", "c101280200"),
        SHANTOU("汕头", "c101280500"),
        SHENZHEN("深圳", "c101280600"),
        SHANWEI("汕尾", "c101282100"),
        SANYA("三亚", "c101310200"),
        SANSHA("三沙", "c101310300"),
        TIANJIN("天津", "c101030100"),
        TONGHUA("通化", "c101060400"),
        TIELING("铁岭", "c101071100"),
        TONGLIAO("通辽", "c101080400"),
        TANGSHAN("唐山", "c101090500"),
        TAIYUAN("太原", "c101100100"),
        TONGCHUAN("铜川", "c101111000"),
        TAIAN("泰安", "c101120800"),
        TULUFAN("吐鲁番", "c101130800"),
        TACHENG("塔城地区", "c101131400"),
        TUMUSHUKE("图木舒克", "c101131800"),
        TIEMENGUAN("铁门关", "c101132000"),
        TIANSHUI("天水", "c101160900"),
        TAIZHOU("泰州", "c101191200"),
        TIANMEN("天门", "c101201600"),
        TAIZHOU2("台州", "c101210600"),
        TONGLING("铜陵", "c101221200"),
        TONGREN("铜仁", "c101260400"),
        TUNCHANG("屯昌", "c101311100"),
        TAIWAN("台湾", "c101341100"),
        WUHAI("乌海", "c101080300"),
        WULANCHABU("乌兰察布", "c101080900"),
        WEINAN("渭南", "c101110500"),
        WEIFANG("潍坊", "c101120600"),
        WEIHAI("威海", "c101121300"),
        WULUMUQI("乌鲁木齐", "c101130100"),
        WUJIAQU("五家渠", "c101131900"),
        WUWEI("武威", "c101160500"),
        WUZHONG("吴忠", "c101170300"),
        WUXI("无锡", "c101190200"),
        WUHAN("武汉", "c101200100"),
        WENZHOU("温州", "c101210700"),
        WUHU("芜湖", "c101220300"),
        WENSHAN("文山壮族苗族自治州", "c101291100"),
        WUZHOU("梧州", "c101300600"),
        WUZHISHAN("五指山", "c101310500"),
        WENCHANG("文昌", "c101310700"),
        WANNING("万宁", "c101310800"),
        XILINGUOLE("锡林郭勒盟", "c101081000"),
        XINGAN_MENG("兴安盟", "c101081100"),
        XINGTAI("邢台", "c101090900"),
        XINZHOU("忻州", "c101101000"),
        XIAN("西安", "c101110100"),
        XIANYANG("咸阳", "c101110200"),
        XINXING("新星市", "c101132500"),
        XINING("西宁", "c101150100"),
        XINXIANG("新乡", "c101180300"),
        XUCHANG("许昌", "c101180400"),
        XINYANG("信阳", "c101180600"),
        XUZHOU("徐州", "c101190800"),
        XIANGYANG("襄阳", "c101200200"),
        XIAOGAN("孝感", "c101200400"),
        XIANNING("咸宁", "c101200700"),
        XIANTAO("仙桃", "c101201400"),
        XUANCHENG("宣城", "c101221300"),
        XIAMEN("厦门", "c101230200"),
        XINYU("新余", "c101241000"),
        XIANGTAN("湘潭", "c101250200"),
        XIANGXI("湘西土家族苗族自治州", "c101251400"),
        XISHUANGBANNA("西双版纳傣族自治州", "c101291000"),
        HONGKONG("香港", "c101320300"),
        YICHUN("伊春", "c101050700"),
        YANBIAN("延边朝鲜族自治州", "c101060900"),
        YINGKOU("营口", "c101070800"),
        YANGQUAN("阳泉", "c101100300"),
        YUNCHENG("运城", "c101100800"),
        YANAN("延安", "c101110300"),
        YULIN("榆林", "c101110400"),
        YANTAI("烟台", "c101120500"),
        YILI("伊犁哈萨克自治州", "c101130600"),
        YUSHU("玉树藏族自治州", "c101150700"),
        YINCHUAN("银川", "c101170100"),
        YANGZHOU("扬州", "c101190600"),
        YANCHENG("盐城", "c101190700"),
        YICHANG("宜昌", "c101200900"),
        YICHUN2("宜春", "c101240500"),
        YINGTAN("鹰潭", "c101241100"),
        YIYANG("益阳", "c101250700"),
        YUEYANG("岳阳", "c101251000"),
        YONGZHOU("永州", "c101251300"),
        YIBIN("宜宾", "c101271100"),
        YAAN("雅安", "c101271600"),
        YUNFU("云浮", "c101281400"),
        YANGJIANG("阳江", "c101281800"),
        YUXI("玉溪", "c101290400"),
        YULIN2("玉林", "c101300900"),
        ZHANGJIAKOU("张家口", "c101090300"),
        ZIBO("淄博", "c101120300"),
        ZAOZHUANG("枣庄", "c101121400"),
        ZHANGYE("张掖", "c101160700"),
        ZHONGWEI("中卫", "c101170500"),
        ZHENGZHOU("郑州", "c101180100"),
        ZHOUKOU("周口", "c101181400"),
        ZHUMADIAN("驻马店", "c101181600"),
        ZHENJIANG("镇江", "c101190300"),
        ZHOUSHAN("舟山", "c101211100"),
        ZHANGZHOU("漳州", "c101230600"),
        ZHUZHOU("株洲", "c101250300"),
        ZHANGJIAJIE("张家界", "c101251100"),
        ZUNYI("遵义", "c101260200"),
        ZIGONG("自贡", "c101270300"),
        ZIYANG("资阳", "c101271300"),
        ZHUHAI("珠海", "c101280700"),
        ZHAOQING("肇庆", "c101280900"),
        ZHANJIANG("湛江", "c101281000"),
        ZHONGSHAN("中山", "c101281700"),
        ZHAOTONG("昭通", "c101290700");

        private final String name;
        private final String code;

        CityCode(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @JsonCreator
        public static CityCode forValue(String value) {
            for (CityCode cityCode : CityCode.values()) {
                if (cityCode.name.equals(value)) {
                    return cityCode;
                }
            }
            return NULL;
        }

    }

    @Getter
    public enum JobType {
        NULL("不限", "0"),
        FULL_TIME("全职", "1901"),
        PART_TIME("兼职", "1903");

        private final String name;
        private final String code;

        JobType(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @JsonCreator
        public static JobType forValue(String value) {
            for (JobType jobType : JobType.values()) {
                if (jobType.name.equals(value)) {
                    return jobType;
                }
            }
            return NULL;
        }
    }

    @Getter
    public enum Salary {
        NULL("不限", "y_0"),
        BELOW_3K("3K以下", "y_1"),
        FROM_3K_TO_5K("3-5K", "y_2"),
        FROM_5K_TO_10K("5-10K", "y_3"),
        FROM_10K_TO_20K("10-20K", "y_4"),
        FROM_20K_TO_50K("20-50K", "y_6"),
        ABOVE_50K("50K以上", "y_8");

        private final String name;
        private final String code;

        Salary(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @JsonCreator
        public static Salary forValue(String value) {
            for (Salary salary : Salary.values()) {
                if (salary.name.equals(value)) {
                    return salary;
                }
            }
            return NULL;
        }
    }

    @Getter
    public enum Degree {
        NULL("不限", "0"),
        BELOW_JUNIOR_HIGH_SCHOOL("初中及以下", "d_209"),
        SECONDARY_VOCATIONAL("中专/中技", "d_208"),
        HIGH_SCHOOL("高中", "d_206"),
        JUNIOR_COLLEGE("大专", "d_202"),
        BACHELOR("本科", "d_203"),
        MASTER("硕士", "d_204"),
        DOCTOR("博士", "d_205");

        private final String name;
        private final String code;

        Degree(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @JsonCreator
        public static Degree forValue(String value) {
            for (Degree degree : Degree.values()) {
                if (degree.name.equals(value)) {
                    return degree;
                }
            }
            return NULL;
        }
    }

    @Getter
    public enum Scale {
        NULL("不限", "0"),
        ZERO_TO_TWENTY("0-20人", "s_301"),
        TWENTY_TO_NINETY_NINE("20-99人", "s_302"),
        ONE_HUNDRED_TO_FOUR_NINETY_NINE("100-499人", "s_303"),
        FIVE_HUNDRED_TO_NINE_NINETY_NINE("500-999人", "s_304"),
        ONE_THOUSAND_TO_NINE_NINE_NINE_NINE("1000-9999人", "s_305"),
        TEN_THOUSAND_ABOVE("10000人以上", "s_306");

        private final String name;
        private final String code;

        Scale(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @JsonCreator
        public static Scale forValue(String value) {
            for (Scale scale : Scale.values()) {
                if (scale.name.equals(value)) {
                    return scale;
                }
            }
            return NULL;
        }
    }

    @Getter
    public enum Financing {
        NULL("不限", "0"),
        UNFUNDED("未融资", "801"),
        ANGEL_ROUND("天使轮", "802"),
        A_ROUND("A轮", "803"),
        B_ROUND("B轮", "804"),
        C_ROUND("C轮", "805"),
        D_AND_ABOVE("D轮及以上", "806"),
        LISTED("已上市", "807"),
        NO_NEED("不需要融资", "808");

        private final String name;
        private final String code;

        Financing(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @JsonCreator
        public static Financing forValue(String value) {
            for (Financing financing : Financing.values()) {
                if (financing.name.equals(value)) {
                    return financing;
                }
            }
            return NULL;
        }
    }

    @Getter
    public enum Industry {
        NULL("不限", "0"),
        INTERNET("互联网", "100020"),
        COMPUTER_SOFTWARE("计算机软件", "100021"),
        CLOUD_COMPUTING("云计算", "100029");

        private final String name;
        private final String code;

        Industry(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @JsonCreator
        public static Industry forValue(String value) {
            for (Industry industry : Industry.values()) {
                if (industry.name.equals(value)) {
                    return industry;
                }
            }
            return NULL;
        }
    }

}

