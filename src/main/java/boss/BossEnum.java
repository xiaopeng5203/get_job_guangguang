package boss;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
public class BossEnum {
    @Getter
    public enum Experience {
        NULL("不限", "0"),
        STUDENT("在校生", "108"),
        GRADUATE("应届毕业生", "102"),
        UNLIMITED("经验不限", "101"),
        LESS_THAN_ONE_YEAR("1年以下", "103"),
        ONE_TO_THREE_YEARS("1-3年", "104"),
        THREE_TO_FIVE_YEARS("3-5年", "105"),
        FIVE_TO_TEN_YEARS("5-10年", "106"),
        MORE_THAN_TEN_YEARS("10年以上", "107");

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
        NULL("不限", "0"),
        ALL("全国", "100010000"),
        BEIJING("北京", "101010100"),
        SHANGHAI("上海", "101020100"),
        TIANJIN("天津", "101030100"),
        HANGZHOU("杭州", "101210100"),
        GUANGZHOU("广州", "101280100"),
        SHENZHEN("深圳", "101280600"),
        WUHAN("武汉", "101200100"),
        CHENGDU("成都", "101270100"),
        XIAMEN("厦门", "101230200"),
        QUANZHOU("泉州", "101230600"),
        FUZHOU("福州", "101230100"),
        CHONGQING("重庆", "101040100"),
        SUZHOU("苏州", "101190400"),
        NANJING("南京", "101190100"),
        HEFEI("合肥", "101220100"),
        QINGDAO("青岛", "101120200"),
        NINGBO("宁波", "101210400"),
        WUXI("无锡", "101190200"),
        DONGGUAN("东莞", "101281600"),
        FOSHAN("佛山", "101280800"),
        ZHUHAI("珠海", "101280700"),
        SHENYANG("沈阳", "101070100"),
        CHANGCHUN("长春", "101060100"),
        HARBIN("哈尔滨", "101050100"),
        SHIJIAZHUANG("石家庄", "101090100"),
        ZHENGZHOU("郑州", "101180100"),
        CHANGSHA("长沙", "101250100"),
        NANCHANG("南昌", "101240100"),
        NANNING("南宁", "101300100"),
        KUNMING("昆明", "101290100"),
        GUIYANG("贵阳", "101260100"),
        LANZHOU("兰州", "101160100"),
        HAIKOU("海口", "101310100"),
        HUHEHAOTE("呼和浩特", "101080100"),
        WULUMUQI("乌鲁木齐", "101130100"),
        TAIYUAN("太原", "101100100"),
        JINAN("济南", "101120100"),
        WENZHOU("温州", "101210700"),
        JIAXING("嘉兴", "101210300"),
        JINHUA("金华", "101210900"),
        TAIZHOU("台州", "101210600"),
        SHAOXING("绍兴", "101210500"),
        CHANGZHOU("常州", "101190300"),
        YANGZHOU("扬州", "101190600"),
        XUZHOU("徐州", "101190800"),
        NANTONG("南通", "101190500"),
        WEIFANG("潍坊", "101120600"),
        YANTAI("烟台", "101120500"),
        LUOYANG("洛阳", "101180900"),
        TANGSHAN("唐山", "101090200"),
        BAODING("保定", "101090300"),
        HANDAN("邯郸", "101091000"),
        QINHUANGDAO("秦皇岛", "101091100"),
        BAOTOU("包头", "101080200"),
        YINCHUAN("银川", "101170100"),
        SHIZUISHAN("石嘴山", "101170200"),
        WUZHONG("吴忠", "101170300"),
        GUYUAN("固原", "101170400"),
        ZHONGWEI("中卫", "101170500"),
        LHASA("拉萨", "101140100"),
        RIKAZE("日喀则", "101140200"),
        CHANGDU("昌都", "101140300"),
        LINZHI("林芝", "101140400"),
        SHANNAN("山南", "101140500"),
        NAQU("那曲", "101140600"),
        ALI("阿里", "101140700"),
        HONGKONG("香港", "101320100"),
        MACAU("澳门", "101330100"),
        TAIPEI("台北", "101340100"),
        KAOHSIUNG("高雄", "101340200"),
        TAICHUNG("台中", "101340300");

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
        NULL("不限", "0"),
        BELOW_3K("3K以下", "402"),
        FROM_3K_TO_5K("3-5K", "403"),
        FROM_5K_TO_10K("5-10K", "404"),
        FROM_10K_TO_20K("10-20K", "405"),
        FROM_20K_TO_50K("20-50K", "406"),
        ABOVE_50K("50K以上", "407");

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
        BELOW_JUNIOR_HIGH_SCHOOL("初中及以下", "209"),
        SECONDARY_VOCATIONAL("中专/中技", "208"),
        HIGH_SCHOOL("高中", "206"),
        JUNIOR_COLLEGE("大专", "202"),
        BACHELOR("本科", "203"),
        MASTER("硕士", "204"),
        DOCTOR("博士", "205");

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
        ZERO_TO_TWENTY("0-20人", "301"),
        TWENTY_TO_NINETY_NINE("20-99人", "302"),
        ONE_HUNDRED_TO_FOUR_NINETY_NINE("100-499人", "303"),
        FIVE_HUNDRED_TO_NINE_NINETY_NINE("500-999人", "304"),
        ONE_THOUSAND_TO_NINE_NINE_NINE_NINE("1000-9999人", "305"),
        TEN_THOUSAND_ABOVE("10000人以上", "306");

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
