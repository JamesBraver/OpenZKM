package com.zelix.klassmaster.config;

import com.zelix.UIConfig;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.utils.GlobalUtils;

public abstract class Options extends SystemConstants {

	public static final boolean ENABLE_PATCHES = true;

	public static final char TAB_CHAR = '\t';
	public static final String PATH_SEPARATOR = System.getProperty("path.separator", ";");
	public static final UIConfig uiConfig = new UIConfig(SystemConstants.userDirectory, "ZKM_O.ser");
	public static int minDottedNameLength = 3;
	public static final int threadCount = Runtime.getRuntime().availableProcessors();
//   public static final String v = LogUtils.Method1822(
//   );
//   public static final boolean i = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean OK = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zq = LogUtils.Method1822(
//   );
//   public static final boolean hz = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean zf = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String ze = LogUtils.Method1822(
//   );
//   public static final boolean Oo = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean Z = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zY = LogUtils.Method1822(
//   );
//   public static final boolean Oy = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean o = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String hm = LogUtils.Method1822(
//   );
//   public static final boolean ln = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean ld = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String hv = LogUtils.Method1822(
//   );
//   public static final boolean lv = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean zH = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String O9 = LogUtils.Method1822(
//   );
//   public static final boolean z9 = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean lG = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String hK = LogUtils.Method1822(
//   );
//   public static final boolean zx = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean l5 = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String z = LogUtils.Method1822(
//   );
//   public static final boolean Q = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean lJ = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String Os = LogUtils.Method1822(
//   );
//   public static final boolean Oq = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean OS = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String ll = LogUtils.Method1822(
//   );
//   public static final boolean ha = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean a7 = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String U = LogUtils.Method1822(
//   );
//   public static final boolean lH = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean hC = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String hH = LogUtils.Method1822(
//   );
//   public static final boolean y = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean z0 = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zE = LogUtils.Method1822(
//   );
//   public static final boolean Oz = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean l0 = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zm = LogUtils.Method1822(
//   );
//   public static final boolean OR = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean ho = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String lp = LogUtils.Method1822(
//   );
//   public static final boolean aN = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean z_ = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String OE = LogUtils.Method1822(
//   );
//   public static final boolean Of = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean hc = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zd = LogUtils.Method1822(
//   );
//   public static final boolean zC = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean zv = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String z7 = LogUtils.Method1822(
//   );
//   public static final boolean zR = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean zh = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zW = LogUtils.Method1822(
//   );
//   public static final boolean zD = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean Ow = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zS = LogUtils.Method1822(
//   );
//   public static final boolean hr = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean zn = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zM = LogUtils.Method1822(
//   );
//   public static final boolean hL = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean OB = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String O3 = LogUtils.Method1822(
//   );
//   public static final boolean lS = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean Oi = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String l1 = LogUtils.Method1822(
//   );
//   public static final boolean Ox = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean zV = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String O1 = LogUtils.Method1822(
//   );
//   public static final boolean hu = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean Oe = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String Oc = LogUtils.Method1822(
//   );
//   public static final boolean z2 = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean K = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String aS = LogUtils.Method1822(
//   );
//   public static final boolean zT = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean lx = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zr = LogUtils.Method1822(
//   );
//   public static final boolean I = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean z1 = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String OM = LogUtils.Method1822(
//   );
//   public static final boolean l8 = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean L = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String hy = LogUtils.Method1822(
//   );
//   public static final boolean t = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean Og = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String zw = LogUtils.Method1822(
//   );
//   public static final boolean H = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean hk = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String lA = LogUtils.Method1822(
//   );
//   public static final boolean ax = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean zt = !LogUtils.Method1850(
//      )
//      .equals("false");
//   public static final String h3 = LogUtils.Method1822(
//   );
//   public static final boolean zi = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean zN = !LogUtils.Method1850(
//      )
//      .equals("false");
	public static final String ZKM_NEW_CHANGE_LOG_ENCODING = GlobalUtils.findHashedSystemProperty(
		"5ac3af378a1f76f234fcaf85bee2f8cefc88b84ab72edc4f6f55ca4dc8cd2139843c83e92b1e83d7a163143d57c54e53fa92a69d09b429493371907080634f15"
	);
	public static final String ZKM_JAD_ENCODING = GlobalUtils.findHashedSystemProperty(
		"35bbb8a793517e8f0da520c0390454b37da96190ff1a8568061969ab8fee833d892a7c161b6531e55b3090c968fc8102ed20c61d88c05589f584604054d30c61"
	);
	public static final String ZKM_SCRIPT_ENCODING = GlobalUtils.findHashedSystemProperty(
		"5b379c316876954087e761c9fe1ed705a6ec8f3e042aba396311f41bbeda6d78dc8cd0b1aef399788389c10f786be2ce122c9857456967dd2e969936769f3789"
	);
	public static final boolean ZKM_RANDOMIZE_OBFUSCATION = GlobalUtils.findHashedSystemProperty(
			"ea6de8c7d94a37e373802bd5c29a5561f5e033f26709fe7b19675dbeb5f60b7c6f58ee5c8001f85fab5feb57529b755c3a6b3c7278e1ae94f8692b19866686fa", "false"
		)
		.equals("true");
//   public static final boolean hD = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean l6 = LogUtils.Method1850(
//      )
//      .equals("true");
	public static final boolean ZKM_DEBUG_SHOW_OLD_NAMES = GlobalUtils.findHashedSystemProperty(
			"41ca8705c1cb49d968c0af5ff3b01c763d6706c789e56d4bb5b64e7ab778335946c3269cf08aa85e398b84edfab1e7ce3f502c0a7d0649f1ffd3071c7f38c94a", "false"
		)
		.equals("true");
//   public static final boolean zG = LogUtils.Method1850(
//      )
//      .equals("true");
	public static final boolean useGOTO = GlobalUtils.findHashedSystemProperty(
			"66579db682edfad7964db301de5047e656aa2ad1b8296fa3b23e5179224507f43e971b990220c599ba22240ac06ddd80a5505d201f81b6b590ef9b1583c14920", "false"
		)
		.equals("true");
	public static final boolean ZKM_MARK_AS_SYNTHETIC = GlobalUtils.findHashedSystemProperty(
			"6a025ba242cba58eddb5f91aac17a24ced9b19fa92513d8e3f527de4872dfe64b2d038919fed416c87e52af81db0975bdd0753fccac96a7426ea7b884edaa3f5", "false"
		)
		.equals("true");
	public static final boolean ZKM_CREATE_BACKUP = !GlobalUtils.findHashedSystemProperty(
			"009ca4a115abc39cb5fbbd43be46e2f32ba7574b9fa8b2577a892251c12d6c116f18af1c4ad34674b721f8bdf5f034b5958067a0ce9cb2dc062d3b77b6826897", ""
		)
		.equals("false");
	public static final boolean ZKM_FIXED_TOTALLY_UNCHANGED = GlobalUtils.findHashedSystemProperty(
			"22bcbcd0fdb7887ae0f4526a4997393c9790c22900572521a5d5411100f6c8e165a88aa40538a86babe973abee45e3e913e4b8662413e970285fdf5d03607833", "false"
		)
		.equals("true");
	public static final boolean ZKM_REFLECTION_WARNINGS = !GlobalUtils.findHashedSystemProperty(
			"3f8d8478e330dcad6914faa6cc2bc3162175aa2e3fdf8faf4db35c1c50c31574111270699c684fc8997473d49fa6ba26644389387778f231a9a233e3e274f4ef", ""
		)
		.equals("false");
	public static final boolean ZKM_TEST_HIERARCHY = !GlobalUtils.findHashedSystemProperty(
			"53a9fe4e1d464298ddf67b1cb4b364997e1635e7cb9d0dfc67fcbdf8859e6df02608507ecd4871b1814ebe3614bf282b7e36774448e84e5a057f2af5fc92e690", ""
		)
		.equals("false");
	public static final boolean ZKM_ALLOW_RENAME_MAIN = GlobalUtils.findHashedSystemProperty(
			"09a0eded4e42f5c3c4d97866d1e3ac8d2c07db44e93e888b12afe081f2efa565e3f2a6b509d48eaaedbdc2702dcfe3bb63c3ff461415ca2232c2c5867aa04c44", ""
		)
		.equals("true");
	public static final boolean mainClassAsEntryPoint = !GlobalUtils.findHashedSystemProperty(
			"402970e499e1a345dfac6cca6fd84aeaf79b4abe369782bcf47489c303098be60f3981957789214adb6b0b68d280f747be15f141d702059b5c6762e44e425653", "true"
		)
		.equals("false");
	public static final boolean ZKM_DELETE_EMPTY_DIRECTORIES = GlobalUtils.findHashedSystemProperty(
			"e2bc6a191dec4964e4b8218e637f063cd6119f7c707b170d035e204e1b5baad6a55e30781553255bd004c57d625a52a48ea637441a12baef470e40d4f6f085ca", "false"
		)
		.equals("true");
	public static final boolean excludeResourceEntryPoints = !GlobalUtils.findHashedSystemProperty(
			"ca7a2e00e438b7202aff872e520b8f2c5599d1b193d612e61532ed168e773cf21594c03835e5f345b47b5e4e3a7c3250f54f87b7fed110d4c231769ee55868d4", ""
		)
		.equals("false");
	public static final String ZKM_EXCEPTION_OBFUSCATION_USE_EXCEPTION = GlobalUtils.findHashedSystemProperty(
		"1b29989ee6484107d911d591f4311fe23001fe70e6b10b159674eeedfb8d60c105de2e4b21d0d9741c299b794a255ef06aef4f57897e189b5da11a6a2acb78ac"
	);
	public static final String ZKM_EXCEPTION_OBFUSCATION_DONT_USE_EXCEPTION = GlobalUtils.findHashedSystemProperty(
		"2d10624884df849bdda8397d3baf194c804f89b57ee419b010754e562931a19e6fe61758f72680a97d991aa8e26f6ed7564d6f9613bbfbf25e916edcae8e1c98"
	);
	public static final boolean ZKM_TREAT_EXCEPTION_OBFUSCATION_AS_FLOW_OBFUSCATION = GlobalUtils.findHashedSystemProperty(
			"b5029b7ef4642c377addef9a3f3ed30e4bb2259638e797fa0d2f92d74a4f99b71e9a290176964e695c8c0eeb6edc08a67c042adc3eb5a980df3db3f6e96d1dce", "false"
		)
		.equals("true");
	public static final boolean unusedOption3 = GlobalUtils.findHashedSystemProperty(
			"b8288aa599af0d36d92e3c3f223af6b64ec430486783c933f4db81a72955c338dc7ed82607e30f855cd375cb4fc7ade0c00b686ca00040dcb015061bd69e7733", "false"
		)
		.equals("true");
	public static final String ZKM_STACK_MAP_ALGORITHM = GlobalUtils.findHashedSystemProperty(
		"320b3f3135ed48d14e719bf8b4a70d3ce35ce4f5bcb1d30261658b4d87d1d427621def69871319bb9dfd93f6f188b9937e9691f03b42dffc22a0aa24c6ae7129", "0"
	);
	public static final boolean ZKM_DEBUG_FORCE_FULL_STACK_MAP = GlobalUtils.findHashedSystemProperty(
			"04863e64f8a70981ba4ea520739d520e07f0c8b7574b04edad570fda9a7881fe7e46a42100b11ae5de2bf7d0ae766b57546b6157c499dde03187aa9a3763a6f2", "false"
		)
		.equals("true");
	public static final boolean ZKM_REBUILD_STACK_MAPS = GlobalUtils.findHashedSystemProperty(
			"fb051fec4a63f6b776eea8c43911d4e3c49fecb080395a666cad5dff22c8141d04fff5bb92fa5d23b09efd541f794b7a46a978b47b3c7878f860df55a9c2f224", "false"
		)
		.equals("true");
	public static final boolean ZKM_IGNORE_MISSING_MEMBERS = GlobalUtils.findHashedSystemProperty(
			"c3e3da6dfc04c0061f11c67aa405992f2eff9fd9d91e3dfb968efea8c1ac4db0b08915854bd51118241ef6f730688b90a991cb450cb5c285f86337791953d5c6", "false"
		)
		.equals("true");
//   public static final boolean V = LogUtils.Method1850(
//      )
//      .equals("true");
	public static final String autoReflectionMinLength = GlobalUtils.findHashedSystemProperty(
		"dbbf2a46360c83c1e267929032998a58263f1fefe46d42aeb6ad99537e5ece878a35874b4b819c54fbae5b3d617c39c62c523ef2c6393845ed6faa29260d0d94"
	);
	public static final String minAutoReflectionEntries = GlobalUtils.findHashedSystemProperty(
		"95d6b799e642d8895963f08ce6df98a6c640a0fa6447860474a179b724998594959d17e8965990d248ce45ab53da75a5a0e922abde8fd4b9b79be509a4d2882c"
	);
	public static final boolean analyzeAutoReflectionClass = !GlobalUtils.findHashedSystemProperty(
			"f888e8b13c3caaf2dbddc1a4dfdd78ef206399657e191409d075289b032ee8c1b117a61a8b8c433c0ea9b2083a6aea4c4b77dd63d698325cf14b70d04c6cc13a", "true"
		)
		.equals("false");
	public static final String ZKM_RANDOM_SEED = GlobalUtils.findHashedSystemProperty(
		"9c6aa07bfdb5188e69d5e22c528ade723d655ecac87289a4053caad541710ffe04b85765690cda8076da09adb43dac6a3fcf4c5740399804d7ff2f98cd5890d3"
	);
//   public static final boolean lP = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final String zs = LogUtils.Method1822(
//   );
//   public static final boolean hg = zs != null;
	public static final boolean ZKM_PROGUARD_STYLE = GlobalUtils.findHashedSystemProperty(
			"7f332084bcdad92af8485f3b485b17078bfe34370d75ae52dced9d0f532108e1d6a9373cbc0f0d5028025f56c99df5c6f2686eae97cccdc3d5cecc8209376ad2", "false"
		)
		.equals("true");
	public static final String ZKM_DEFAULT_DIR = GlobalUtils.findHashedSystemProperty(
		"a7f508eeb1fd90ee8bad1fdce431714ec8e22d51aa08ffdaecb1db4526fad6aeaf6328a6bc2b96b42adee291a0852bad8f7e597c37e46918f8e647c2a4174368"
	);
	public static final boolean ZKM_UNIQUE_INNER_CLASS = GlobalUtils.findHashedSystemProperty(
			"109f3bee852d498531318089d48840df88ba30e409a3e4bec95e1ab719704922aa4aea79a553b7a4d94958ec247274bc039d1c419e93e41c325e3d22a64094c0", "false"
		)
		.equals("true");
	public static final boolean dontDuplicateConstants = GlobalUtils.findHashedSystemProperty(
			"58a15191485daa77b6600faa674e4eda1c896b543fd29ea6dcb413ed3f51766b6244a1421cdff95d21047eb5fc8861722aca1514287541d570fb84d8edcb73e3", "false"
		)
		.equals("true");
	public static final boolean allowDuplicateManifestValues = GlobalUtils.findHashedSystemProperty(
			"bed713aeb8269261a5703de9bd84e2f6bdca92fcfaabb45f2fc0675483c1a72bf5e95010f604f29f628dfc0f596dc3fa0684031b75a43efc481657073daca385", "false"
		)
		.equals("true");
	public static final boolean ZKM_FORCE_AUTOREFLECTION = GlobalUtils.findHashedSystemProperty(
			"4a4d89b756f2d5f6e05cba65674ff6597f5856d57b2c41d0d847a29a7368c4f0a3c7f8a707166cd4667642bab3541073e6434f8123ce86a9c1f919be8840cdb6", "false"
		)
		.equals("true");
	public static final boolean ZKM_PARSE_ONLY = GlobalUtils.findHashedSystemProperty(
			"e57109a154191b161c22e69f02ab56e78af5c7fcba9737b6ecda7c685daf5ac630ea741dd38b565c56a08aaaaf92515476335783bbd6bfa665edfdc01aae53cd", "false"
		)
		.equals("true");
	public static final boolean ZKM_ADD_MANIFEST_CLASSPATHS = GlobalUtils.findHashedSystemProperty(
			"d0612a1ad0fad36dddd08084f49f41d279dfc5af129a9b92fcc12281513b1b90adfe1ef68921f5f7631c78c602e42066f66e7a678239bb66b6b753d3e8b8382c", "false"
		)
		.equals("true");
	public static final boolean ZKM_DONT_ADD_WATERMARK = GlobalUtils.findHashedSystemProperty(
			"f2ebae5e8608e0086a923841e0a533c5fe5f21abd8d85e596188531305e3bafbc8277d67137de8e422348f1962a0f3e9fd8657131ce3d9779950d5ac14ea2882", "false"
		)
		.equals("true");
	public static final boolean useRefClassForWorkspaceNodes = GlobalUtils.findHashedSystemProperty(
			"15df04ac9d900a14fb2c40406a4c31b682c34b6fe580c35d4fd91639c0b2d55b3895958e796ed9ef2b152d4f7a94fbb97fab97bd962566f9dc040ef8b579f2d9", "false"
		)
		.equals("true");
	public static final boolean ZKM_OBFUSCATE_REFERENCES_OBFUSCATE_EXCEPTIONS = !GlobalUtils.findHashedSystemProperty(
			"af745bbfdce8032ba90d45d69819cae6ea103b91a81f0ac6a9d5f977eb0a39872cc21aec855d5f30394f9cb9c5a12458b9b8c710f7d8244132f9ad8ad4ae9a17", ""
		)
		.equals("false");
//   public static final boolean zI = LogUtils.Method1850(
//      )
//      .equals("true");
	public static final boolean treatReturnValuesAsUsed = GlobalUtils.findHashedSystemProperty(
			"a50c593868b351feff1aa8c06847cb76d11b9654ae2dcb4015abb0e5221a2ee58f5aeeba55dee118e500471e80557bfa9840a902cfd0cf14d1fcc926c5fd2806", "false"
		)
		.equals("true");
	public static final boolean analyzeReferenceDataClass = !GlobalUtils.findHashedSystemProperty(
			"9067176c40d247b4bce0cf36bac8252b5e2a5d533506e474ed982d5ee47c75581a4cb5b794a1b12a6984598d58721c6574f71204ec4851c0c71211f107d2db45", "true"
		)
		.equals("false");
	public static final boolean useSpecialStackMapFrames = GlobalUtils.findHashedSystemProperty(
			"70d795b5e4943631a0d1958b2ee0aa403849e658643341f48306f23f66dc25354e06e9db3e1211e7d15f4eb3dd90e58587134d7c981caddb565eb164541e7841", "false"
		)
		.equals("true");
	public static final boolean ZKM_WRITE_EMPTY_MANIFEST_VALUES = GlobalUtils.findHashedSystemProperty(
			"5f8dd870ecd6174bbb7d383f714e19a79bafefb582ace06dd05da7832133f7eb21a662fa3dafc5c55a44fb019b4a3317840fc45002c861b0db67f5bb943c47db", "false"
		)
		.equals("true");
	public static final boolean ZKM_TEST_JVM_VERSION = !GlobalUtils.findHashedSystemProperty(
			"1e61827545955ee0704f99b8a9b8c3c00221604b9021b8ff7cdff05f49c07e98a7602811efcd5a393405d326b6b05dd7700fbca99f4ad19844c55a677f57a650", "true"
		)
		.equals("false");
	public static final boolean ZKM_DEBUG_REDIRECT_OUT = GlobalUtils.findHashedSystemProperty(
			"6cb2fbc8c8d490cc840c77be4b73d642f99633d345c9e4218e1bf5ef40531e96696b2303363ce17f92d036c8162c95fd4e58aa04c10412e20dfe3f487deb8686", "false"
		)
		.equals("true");
	public static final String ZKM_AUTOREFLECTION_MAJOR_VERSION = GlobalUtils.findHashedSystemProperty(
		"90b62b2c202c6d8c7e6acee6c8192bc27700016b833fd617243afa327c4d32800206ef638489e5d7c8760ccd878fb3188feca6647d9cb556d0287d02ac6c6e31"
	);
	public static final String ZKM_AUTOREFLECTION_MINOR_VERSION = GlobalUtils.findHashedSystemProperty(
		"3ac0aaba6f44ca77fa93325807070f48c2b80023707cccfb7d6f1d2bd7bfccf1ad784caae7effbbc91d609dc5f179dc8af9f031ebffa2583531d9417bfbca771"
	);
	public static final boolean ZKM_ADJUST_LOCALS = !GlobalUtils.findHashedSystemProperty(
			"3562dafcc48427b9fbb86328ae202545f4f458c882f0cfbd286c88af37374ec1657d2d17600a181e01a521c3d598ccc5e843e01f117599c3cf529edc254002d9", "true"
		)
		.equals("false");
	public static final boolean checkReservedClassNames = GlobalUtils.findHashedSystemProperty(
			"55243ef57468b8e4d431ca61f1f2b0fe7059f9cec53d69883813bfcb6dc920a4f7409f4c0f97692d53109a4c9fccfc38db859fe0b84a01b8b35465c92ff530ff", "false"
		)
		.equals("true");
	public static final boolean requireBooleanIsAccessor = GlobalUtils.findHashedSystemProperty(
			"2eeaecb78a2ac8a20c9418a11b0e9108d8dae79aa0558c52be6089d59a7642c3667d08ecd7c78485dba8477a350b4905f9529229e4b43cfdea20d13e480d9ff1", "false"
		)
		.equals("true");
	public static final boolean ZKM_STRING_ENCRYPT_INTERN = !GlobalUtils.findHashedSystemProperty(
			"a8362fb7cd090a06043d1a5f71aafac118fc9e02db5527578ab5123bbc26eb0cf42ddbe9e2eb1e35b1772b432bd08a8a28bbf2f586e2facf7e68334474c12e2d", "true"
		)
		.equals("false");
	public static final boolean ZKM_OBFUSCATE_REFERENCES_INDY = !GlobalUtils.findHashedSystemProperty(
			"74fc1b18c052b50335d4c60f41277904b96188d2b521e17ebb72a53e21e114220e04b82f278b47496015aa514aea7f9847c47ed5bd5cfdc90302f746ab025e16", "true"
		)
		.equals("false");
	public static final boolean ZKM_OBFUSCATE_EXTERNAL_REFERENCES_INDY = !GlobalUtils.findHashedSystemProperty(
			"0539163c30a633d7a3cc6a7a32f4c2758d4f1a5847bc80c30e496a723ec48ed425a5091569342a2d6c878a341204ab1ec118d04b95d0b050f0edc527b20500ba", "true"
		)
		.equals("false");
	public static final boolean forceLargeClasses = GlobalUtils.findHashedSystemProperty(
			"7feda4444ee618064ba96d7c4098a7f2e4f10799dc91731cd54615fe95be135d0a11c6e2b577d4fa786f0ce6bf1fa94bfed0d156f76af76bad16ad9d10d9e4db", "false"
		)
		.equals("true");
	public static final boolean dontDetectLargeClasses = GlobalUtils.findHashedSystemProperty(
			"6ebc35e1332b6433dd4f6ddcfc48e1af35d34eba8789cc546ab697773162ed63d8580365db1cfef6bfd462dbc0c02d3276671e26643f3b95960b741fc25f7a36", "false"
		)
		.equals("true");
	public static final boolean dontUseStringFlowLocals = GlobalUtils.findHashedSystemProperty(
			"00b3f66ef0b0a5b00f2ad005f46868bf8b2a2da6b0f29faf0918a0223a70dc9586ddcd808fb7d76bf235d476ccdd95fc08851c1531cb55bad435367848af1356", "false"
		)
		.equals("true");
	public static final boolean ZKM_TRIM_XML_ATTRIBUTE_VALUES = !GlobalUtils.findHashedSystemProperty(
			"d20d9a061ce6db900d96e26aab06adf76c4f207fa4398d73dab12e9ce5205097a6ca9d6bf577519ed732331fd96b32847bcf9cfe2a83181043cb824baede9139", "true"
		)
		.equals("false");
	public static final boolean ZKM_COPY_ARCHIVE_DIRECTORIES = !GlobalUtils.findHashedSystemProperty(
			"7e6d8d53be592654672112f5f40b7ad33ccfbbc59d9923640fd14ac28a33b5fcdca01a0f2349c2f9a557e961a31ff6def01fa7c706f6340b59f00abce3fc9f1c", "true"
		)
		.equals("false");
	public static final boolean dontAppendProguardStatements = GlobalUtils.findHashedSystemProperty(
			"c5307e2a70eb81c9783b3f7d2b44712c861cbebeeb19726f108b2fe047883a2b185e15bbd32ff696da1984f54279bee80e7845f7602826deb3f9536d3c73e65a", "false"
		)
		.equals("true");
	public static final boolean smallXorMask = !GlobalUtils.findHashedSystemProperty(
			"abe695e8820dc8181d36a1d37bc767d4d9d2fa688c4c915b62f8dee8a0157361af4869fadbb6fa679d5deac4d0495f9d65cfa8c08adce2e2c528100f6411f6e4", "true"
		)
		.equals("false");
	public static final boolean useFixedParamSalt = GlobalUtils.findHashedSystemProperty(
			"a42584d7612eb76092e3bc4493ff3836f08ecfd75174e162fe746e609e236fdd6ad658c55e05066a8bdd3e1e61a28b7137e65cad205d63bf1bbb2c0ffdcd43aa", "false"
		)
		.equals("true");
	public static final boolean writeMethodChangeDetails = GlobalUtils.findHashedSystemProperty(
			"918b3d87c5e0a842149e40818575598f0a08365236aa5954fb052125287351876600f0b3c8245f91635dc9bc556b238844b628157a2de49e3beb8a511de42292", "false"
		)
		.equals("true");
	public static final boolean antiDebugger = GlobalUtils.findHashedSystemProperty(
			"c8eaf66ef3c7a27381f9062c26129b5232c65664f5624059a980c511ca2ad2b2c294b663a47801f2fedf8b4b22c53ef489f4bff8a2831ed4f382b24e451ac3c5", "false"
		)
		.equals("true");
	public static final String classInitSpecFile = GlobalUtils.findHashedSystemProperty(
		"1116449451721d66a0195b0dcb33cc813f8fdee4f36628258c2d240a0c6a5b960546ef1c541658826941ff062d79260bb33e81d3dd6f60046923c126304e0704"
	);
	public static final String classToMethodInitSpecFile = GlobalUtils.findHashedSystemProperty(
		"e498a3a3ea59e3a0dd5795acf9db529d458a3110188678a62365f8cbfc59a6e85d7f7628f3156340cfc1272e66953a20556ca2cc5901796d29ae3cf6c8700e04"
	);
	public static final String falseClassInitSpecFile = GlobalUtils.findHashedSystemProperty(
		"da34336ad842b4dcb2afd02ebfb84c9a088a119eb14bddc51431829edafe39368b77891c194303528c8ad5643ae7dd6cee5f4034e391480aa4c3d74e36bd1447"
	);
	public static final boolean ZKM_STRING_ENCRYPT_DES = !GlobalUtils.findHashedSystemProperty(
			"4a61d0b8e2107814939e50d5b884abdf39d563d182fce4a2f16db487fd9fd3cd2e8df86947088ec3c91554f902eff2766a5c6de0427a27023555573dea4eb233", "true"
		)
		.equals("false");
	public static final boolean ZKM_STRING_ENCRYPT_INDY = !GlobalUtils.findHashedSystemProperty(
			"bd0e178c755d5057f46cbc633bdcb54240d832d25339cbd1405479cc38dc7396d4bc37e07ead3c48269c70f7d2421af3810145d17a13cf6fec2538d75eadd842", "true"
		)
		.equals("false");
	public static final boolean ZKM_INTEGER_ENCRYPT_DES = !GlobalUtils.findHashedSystemProperty(
			"140b2d472811d0328200681b57064fa6250edc4652b71c123bb0cb20970bfc4d85be1468860c1b3cd957deb1580db6b28330c8b9b10ef792594d67d79e89405f", "true"
		)
		.equals("false");
	public static final boolean ZKM_LONG_ENCRYPT_DES = !GlobalUtils.findHashedSystemProperty(
			"f991be5c9f97d719aba206deb2fb4e8792fa3cd6eb54e88b7a29d772cf0660561ed032a082819cdda0ef79e1671389ea92c2ad4e49c8ca5a15281c8aac05f1f4", "true"
		)
		.equals("false");
	public static final boolean ZKM_INTEGER_ENCRYPT_INDY = !GlobalUtils.findHashedSystemProperty(
			"6e68f6e4a4549582af39045e28f103cec79a734f355a86dd503bb730ec4e8b3a4b39b139710d43074eae6455e80e08729498c91653a36ddf886ce9e1f9e0d1db", "true"
		)
		.equals("false");
	public static final boolean ZKM_LONG_ENCRYPT_INDY = !GlobalUtils.findHashedSystemProperty(
			"5e54ac98d09de3a6006cff21edb35f430f8cd2cf1f210b936929d28968ece95f1a89c37c59887c7ff4ed6580ef9d1cc5e42ac7fcf8624733b2c2914d7ccad531", "true"
		)
		.equals("false");
	public static final boolean ZKM_ADD_NESTED_CLASSPATH_ARCHIVES = !GlobalUtils.findHashedSystemProperty(
			"f8c66157b1461c904108f630d1abe9d245579a400086c707f829b22e6b2b778cb259706296060e2297066ea311d685c64511bb15917bc9ca81dbdbbb38f52485", "true"
		)
		.equals("false");
	public static final boolean ZKM_XML_ASSERTS = !GlobalUtils.findHashedSystemProperty(
			"1ba3a0d62aa26981415e5b8efd56ae7b66076537f9e7d3aeecbbdeda5670f4b19885d4632c8b28fa5e9b9e15b7c925c39ae2dde3669c3aa5eeee94336e8d4212", "true"
		)
		.equals("false");
	public static final String ZKM_LAF_NAME = GlobalUtils.findHashedSystemProperty(
		"f0fa985b8bab087abce4916b6a779bff3baffa4d3899e851b50be1ca9d1a61ff1ed169caa4cc853c502ac7771201c040df52a6aeeb90b5432fcaaf3e2f67acf7"
	);
	public static final boolean ZKM_METHOD_PARAM_CHANGING_LOOKUP = !GlobalUtils.findHashedSystemProperty(
			"d3babce40e1cff07d77551c3307de88ed52af815a38b287caa1de63acdd3c0083516887f491843badbb7f62febe7be5df23959d706ea97942e74501965870041", "true"
		)
		.equals("false");
	public static final boolean ZKM_MATCH_ON_ORIGINAL = GlobalUtils.findHashedSystemProperty(
			"31e372e46eb8a2f01ca90c22cb24e523a215ce7d61343f60e64244ddb92cfef5b80c8b5b062babc1580fe9ee8fb80ac1a55ed4c06cebfcc8c59c421262f6c81d", "false"
		)
		.equals("true");
	public static final String ZKM_METHOD_PARAMETER_MAP_CLASS = GlobalUtils.findHashedSystemProperty(
		"2e185fde20fa27b2b6d1da9d7f9a19c126bb3d8dac279de48b6d10448767dae06af4bf9a33f6fed443e8b3d5c858c066a61af6f8503ca32fa4fc56a2f4409f60"
	);
	public static final boolean ZKM_USE_PARALLEL = !GlobalUtils.findHashedSystemProperty(
			"9c7e0172fe226b5c5b23c7152cc7ea407d25ce4b78fd8d175d10853e015b909dce04fdd2b6959477da32b5fd3015955ded17f6df4fd7951dcf4a4684a8e8bf3e", "true"
		)
		.equals("false");
	public static final boolean ZKM_METHOD_PARAM_SYNCH = GlobalUtils.findHashedSystemProperty(
			"b8add9cb94552e654522a2a01ff41372cc973dc4f5e1b99a3d46d6252c9d29502938804aa2617bd8990b866a7f3fd25de7cd73572b50a0074a5a6c308878f233", "false"
		)
		.equals("true");
	public static final String ZKM_EXTRA_XML_FILE_TYPES = GlobalUtils.findHashedSystemProperty(
		"f218a38a9cf81839b70a79b7339e655a61fb001cccf7985f5ce8489f34ba2b03086310de9a87d135cf08b1df5a79960fde4da25750d467713b373138356fb024"
	);
	public static final boolean ZKM_TRANSLATE_KOTLIN = !GlobalUtils.findHashedSystemProperty(
			"e447e08fd7711fff871f1e72a13c96f52ccae52e242b1dea3227bbd28017da7deafb4aeec0ddab15d4ae419a5b5ca681531ca5de7e1648e3451ebbc3fcfc31be", "true"
		)
		.equals("false");
	public static final boolean markParamsAsVarargs = GlobalUtils.findHashedSystemProperty(
			"0dc26e11372cb56ba3576b3b7e895ca961974f0adedbbb541fdb00fd0e07f7ee3aba128a97f0f0ffd0f56f7e1aeab5881b4e27e15d9a4b550fa7175974de6ab3", "false"
		)
		.equals("true");
	public static final boolean usePhantomClassFallback = GlobalUtils.findHashedSystemProperty(
			"bf9328237364c865a68b59db382e9196514da10d2f4bbebef53a5dc7cdf41e330dcd42185b794633801197577a610e533de7308ae86e9f637028f6ca99ecdc1c", "false"
		)
		.equals("true");
	public static final boolean ZKM_PROCESS_YAML = !GlobalUtils.findHashedSystemProperty(
			"5f7668e17e634001e8d6c10dcbfa2786b6bff4ef82195fcd0347cfcae4cce081effa3c39f5008e23e32a57a38d12121d49992624fc8d9b101970db224a325c3b", "true"
		)
		.equals("false");
	public static final boolean ZKM_PROCESS_PROPERTIES = !GlobalUtils.findHashedSystemProperty(
			"a59376f3080b9cc8b7e470adaca7ee50cb91d110e004c6b2aef80bc60eecc57e061695d2f35a9460021cb1fc7c4b276edd20e615329f86b36443511e3ddc1d1d", "true"
		)
		.equals("false");
	public static final String ZKM_PROPERTIES_ENCODING = GlobalUtils.findHashedSystemProperty(
		"03d2bca23bb3ce9310ee4284010132ebe7c079a29b2c2c99e3d0cd0ba6ed817fa63c1a9e723257313eb1cafee6e4ba2c8286ff4489b32e475cec98cd85914b15"
	);
	public static final boolean ZKM_REPRODUCIBLE_RANDOM = GlobalUtils.findHashedSystemProperty(
			"853a2485343d95055264055e358ef9e6e97e935f80a1c5ec0f5cb4d9f8977fa51c790aad44beb946de27c99d8d0916543e1aa54b7a147113e33bcacfaef5a31d", "false"
		)
		.equals("true");
	public static final boolean ZKM_INTEGER_ENCRYPT_LOW_VALUE_FIELDS = GlobalUtils.findHashedSystemProperty(
			"b727bdefb22bfd439d89430e1b11793d0a6e6dcea2fac4fe96855037913f401143bb6a2d803c5ad5e056db197e5feaabed9c0c42d93c873e5308ec80c43e5897", "false"
		)
		.equals("true");
	public static final boolean ZKM_LONG_ENCRYPT_LOW_VALUE_FIELDS = GlobalUtils.findHashedSystemProperty(
			"8f2a5de3582ae2747d8d82137dd8977c4895dbe621b1e1dfd0611db0ef94f2b410c4518983a35361c7d247e74f4b1db74a225279a152ccec6395110404a6ee57", "false"
		)
		.equals("true");
	public static boolean relaxStackFrameCompatibility = !GlobalUtils.findHashedSystemProperty(
			"b90e12da2adf99f479ecad90a530ea33b8701422fbbe250b5d5f8edb8f7096c8b61dba24a593c4406c81f08cf9f129d95040be9487d1c7f1b0a53713dd37cb2d", "true"
		)
		.equals("false");
	public static final String stackFrameCompatibilityOverride = GlobalUtils.findHashedSystemProperty(
		"3f1abfe352d1aa2b02e56a0383c1997413b95d38241dee43f0cfd442e8d7d7a625c1e767296d163290d0069ba20661ff932ab39edfc3ebfbd1b15fc7bd071366"
	);
	public static final boolean ZKM_INDY_PARAM_CHANGING = !GlobalUtils.findHashedSystemProperty(
			"f3847e56847cfe1710227d665cd29a661ac65579a2dfa0d35f252baba61d007cb98d630eff63b2e9718d4c4a4e9dc8dd18a4fe81031b52ecc71ac24c45fa0740", "true"
		)
		.equals("false");
	public static final boolean ZKM_KEEP_SKIPPED_LAST_MODIFIED_DATE = GlobalUtils.findHashedSystemProperty(
			"8c99232262bf3f9fb4ae50d16bad02cf40c59a1c51ca3e3673cd524663ab6a0efc95c7d632be8942119310373a3a6fb390f81874e0140c67113cc3cc95fc3ce7", "false"
		)
		.equals("true");
	public static final boolean dontInsertFrameBlocks = GlobalUtils.findHashedSystemProperty(
			"2f163d41b138629c77be62ca4596f612549b83f8f63e8ed914b5471124cea3ac6048144ad8cfad38c1120c6ed5bf261c1f244b952ecf085f48e155b23f1c4617", "false"
		)
		.equals("true");
	public static final boolean insertSwapObfuscationBlocks = GlobalUtils.findHashedSystemProperty(
			"aff9b87be34bf5166261e13af08363fde4b3c7ba07ac55dd0fb238e4019f615e7902052f776aa594fe126781a3d0ec936702ad7cc7905e3e1d72bb756bcf0d76", "false"
		)
		.equals("true");
	public static final boolean dontCreateExceptionHelper = GlobalUtils.findHashedSystemProperty(
			"5155078311f8a21deeb48a0036c6d7a736fee65f5ab608afeb7743b4501b73187dd4df87ae8c163b959a62c57e58cc438f5bf9ee39abf340ffc848af7f387666", "false"
		)
		.equals("true");
	public static final boolean skipProducerAdjustment = GlobalUtils.findHashedSystemProperty(
			"07609958474283d41f037a3e9117604a120374870c119de7c0d5a551bb8495447c44d860f9d76a4d0f38add7b4c9d82c84e8b672c48ab15360fb5a5e45707683", "false"
		)
		.equals("true");
	public static final boolean dontHandleStringBuilderChain = GlobalUtils.findHashedSystemProperty(
			"21c22120d116b3bb4c0d0b5f8fce8d0a586f81352ab0f0ac4fd34b67803d9e0211482269973bc38549673387f8515f5fa35b44defcf24833459e4a62e481ab5f", "false"
		)
		.equals("true");
	public static final boolean dontCheckImplementsClause = GlobalUtils.findHashedSystemProperty(
			"2e335299b90e925185cd9df93c4c792b2c06a19339615e70b562719a93ec0b4ca67e51cebb49961464edde4c168d301560ddd67cf5baffe46bc75b6962b55a49", "false"
		)
		.equals("true");
	public static final boolean checkImplementsClause = GlobalUtils.findHashedSystemProperty(
			"35cceb8a66ee4ada0b957b379ee0c0807926639348e102738f3ec878c7d5e02aed212280aac67a146a00c248472b05390bbbc8a30c03836926c83772928ab9a3", "false"
		)
		.equals("true");
	public static final boolean paramChangeAllMethods = GlobalUtils.findHashedSystemProperty(
			"4d71c9e6edf3930a141a46c2f6949dbd30fb312181896b10038e37c84d5901d8e673ff5ef3a48f75d74f42188e8f3c3c599bd981219c3c71ec019686fcce31b0", "false"
		)
		.equals("true");
	public static final boolean ascendingExtraParamOrder = GlobalUtils.findHashedSystemProperty(
			"246be466a92e368372506030668f489bd77dce6dd8d86a5b8936aba3df7fc7236365c66539125390d993bfb70be64006a606e522954086fad447f05a4a2672be", "false"
		)
		.equals("true");
	public static final boolean prependClasspathEntries = GlobalUtils.findHashedSystemProperty(
			"f9ae0d3777e1dfe0d51cb7108805feb9ad94ab536b02cd5aa823c10cfb48b738e486601ae9249322a97b35e91baf4e00d006c620c7542fcd2f0ceced3089fff4", "false"
		)
		.equals("true");
	public static final boolean verifyStackFrameTypes = GlobalUtils.findHashedSystemProperty(
			"c9bd2980414955e70c4fe8652fb0b656088434dbbebdee124bb232150400e66234358d53a31a7da4e223b90501f685779ab095b69068373928f457005cf75414", "false"
		)
		.equals("true");
	public static final boolean forceMediumClasses = GlobalUtils.findHashedSystemProperty(
			"a561945c4a9464698c16e3511ee4be02435408be480df7f2679f8c1b820492c0d40ee46843a4472ac478be129fecb3993748252740ee55f248460fb51d9dbe0e", "false"
		)
		.equals("true");
	public static final boolean dontDetectMediumClasses = GlobalUtils.findHashedSystemProperty(
			"64a2fbea7300e21637d41b78c774671bfca3b41c1b5cd68df3ad9728b069a4b9227700a05eb11467a0cf2ec527f39c17ce68ece7686182e913280d7fc9d2c258", "false"
		)
		.equals("true");
	public static final boolean dontUseIndyReferences = GlobalUtils.findHashedSystemProperty(
			"1fbb7ff52ce9431a4f0e32ffaed05b7c00243a12f9618a71e6be911476c06dbb71f1fa412e1175a89e80cb0ae4e1868c330cfa11a11437e668324b93876a0978", "false"
		)
		.equals("true");
	public static final boolean useStaticBlockEligibility = GlobalUtils.findHashedSystemProperty(
			"8b5a54d1846f3f743eb84d77fc5405d03fec4ad55782c00f49c5abd721f8ed62ff6b4803b9d59687fac8dbd66e3f8fc87d75d2f5eb9aeb5f8b4ad315541f82db", "false"
		)
		.equals("true");
	public static final boolean fixConditionPolarity = GlobalUtils.findHashedSystemProperty(
			"278a365d0df9fb144fb2a813b6823ad00d78ffefa0e02dbe76d3af484cee7d16523e8c304499032707cf80868234c8b3657180ac62a3b1d3c4e2465f722a1795", "false"
		)
		.equals("true");
	public static final boolean useObjectFlowLocals = GlobalUtils.findHashedSystemProperty(
			"bfb6fffedad13fdafa2276940ef473995b79575e6a551bb7707d72331e36fc2c58ebfc804fd4c67c4b12c7c2d81ba850f070c6198e15e27f2d6eca1071234f19", "false"
		)
		.equals("true");
	public static final boolean skipMpcWhenNoCandidates = GlobalUtils.findHashedSystemProperty(
			"2271cd52d51697bc3feadd2bf264e4f81e949b3d96cf3a3b70c97a957fd92e70afb84de9ac0064fd8295155fa6de4a6e5e9f1621d0cf559aa19432351ebb455c", "false"
		)
		.equals("true");
	public static final boolean shuffleClassMembers = GlobalUtils.findHashedSystemProperty(
			"5cde8881cda1b15ae4e8f5eee965e0506e5da0b6bd527568a2ce6ba47cd9f0ed07b2c3292989796f923fc708323cc7c52f95bbde8c56189e0ff140bab0394c0d", "false"
		)
		.equals("true");
	public static final boolean forceParamLookupMethod = GlobalUtils.findHashedSystemProperty(
			"3c389453be425689726c0fabc266377cce72838dccb1b33680b3de1a84111699141dce1b126ab266026da9edece30fc9428a84e48c4234bb6f85822b4eb33410", "false"
		)
		.equals("true");
	public static final boolean dontRotateReflectionParams = GlobalUtils.findHashedSystemProperty(
			"d2c3aa84f034be3ec979f3c5d325c61584f595d50005ed857f388dadc73f514e68026387e831eb242ba4a17ebff51ec541ff774539267297690a869711abde62", "false"
		)
		.equals("true");
	public static final boolean skipSaltCrossLinks = GlobalUtils.findHashedSystemProperty(
			"a13f8c5cf83fa59733b8b5da1c5fa848396918338a1335ce3b1b3d81bfd139119b200227df5d8cbee90d5b30543ff0736e25bea53ddf81c6f2064239da529116", "false"
		)
		.equals("true");
	public static final boolean dontUseParamLookupMethod = GlobalUtils.findHashedSystemProperty(
			"ad734fcbdac707b79f1a19ce2a421f375b02b2dae70b95808e8dccf8e3fbf5cd8282cafa4cb231a0b048215dd4c42c153192b67e343ced01536dfed4c32fc19e", "false"
		)
		.equals("true");
//   public static final boolean hO = LogUtils.Method1850(
//      )
//      .equals("true");
	public static final boolean skipHierarchyClinitPass = GlobalUtils.findHashedSystemProperty(
			"773efd28d903c6d6dbada96f86e9248c025f155e9e497cc4ffa283da6d4a37867dea69f0e70721349adb3021915aa23b9fe6d43865b898e08a0102131ef1c32a", "false"
		)
		.equals("true");
	public static final boolean skipClinitDependencyPass = GlobalUtils.findHashedSystemProperty(
			"22d6a20b1d5712b66cb19dea11da39bc8be8d5323769d1b6ee8c4705b1aebba98246f6a1be6ac8b26bf161b809fdfc46847e2327c68236ebadbd176238e0a7d9", "false"
		)
		.equals("true");
	public static final boolean skipClinitDependencies = GlobalUtils.findHashedSystemProperty(
			"bdb7c0f55c94b78455a3a6ff89aec3ce2e23086310529879d05284d7c2e3c74043db6bd578048059b025c07777390ed9ddf7cd7e6305e496d14865e62a208a20", "false"
		)
		.equals("true");
	public static final boolean dontRotateReferenceParams = GlobalUtils.findHashedSystemProperty(
			"cd10a4090f951e29d84120bfab3b309a3dfc756f460cd7c3f3b2b76d9e779264112ce442c3f6b9b20584bff26c1f5c69ff022012d400c27e73b21903eae56970", "false"
		)
		.equals("true");
	public static final boolean dontEncryptGeneratedMembers = GlobalUtils.findHashedSystemProperty(
			"fb08892b1fd749cc3a4584807d512926f37e9f4692a13bd4be09a2c682312df08d1bd194cd1840922aa1acabaebcd915d111af82be9d645d067f038f65957637", "false"
		)
		.equals("true");
	public static final boolean xorKeyWithLowBits = GlobalUtils.findHashedSystemProperty(
			"2a56675d34e083cc74eff5d3740208e664455182a73957049f1bac73194668994cee762af4c9a525ca5c95d2b1a60cdef3ef8a2579a541425b1be19017b23fdf", "false"
		)
		.equals("true");
	public static final String minSaltSize = GlobalUtils.findHashedSystemProperty(
		"99478180fee01986c237900c508a44be2e99c225e20888ce6da5a8b497e2bc8507e9f0c6e36a0a989c61cda3d3f4d62b96c76b11d5d9e4fe6afe03af6826264b"
	);
	public static final boolean dontChangeMethodParams = GlobalUtils.findHashedSystemProperty(
			"3b4828bfd11d7672cae28f829cf1e2dc07ac151fbe576ea9d73f7a4309729e7573e28748c5c0b2cceac252941c804ded5dee7673b46d6fdf850f876b94cae714", "false"
		)
		.equals("true");
	public static final boolean dontUseMethodHandlesLookup = GlobalUtils.findHashedSystemProperty(
			"4fe8bf83d299a7f0db6adab79ca0a392e51c95a3577781df829ed4a677964795c9712401223241ec80bb2e283f69853eb549abb588d32e3adf3f487312cd9718", "false"
		)
		.equals("true");
	public static final boolean dontChangeReferencedMethods = GlobalUtils.findHashedSystemProperty(
			"af71831c23086ce5b4d278a32631fd44a5305c7a53b977b1fc2db5f5f556964e275eb01c948ad9e5170e204482d30dadf06e0d000b1ba0f40b60e0fc06258db5", "false"
		)
		.equals("true");
	public static final boolean dontUseIndyOnJava7 = GlobalUtils.findHashedSystemProperty(
			"91916b226afd0cc7e24ed4aba9f3e73c5e8074efc5b974661f4c62dfac185d50f9ac2fadf1d1b7cbce34e7d505ed3f93a884d298d7fda027a449fdd6dca7579c", "false"
		)
		.equals("true");
	public static final boolean dontTreatHashAsComment = GlobalUtils.findHashedSystemProperty(
			"8ce5c351d80bf620317587662c00ecf734a781e6100cb17c514583213040b799c3eec2357bfacca63bbf57ace49870aec3c3070735aee647aa200c874b4470ac", "false"
		)
		.equals("true");
//   public static final boolean lt = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean hR = LogUtils.Method1850(
//      )
//      .equals("true");
	public static final boolean dontShuffleReferenceOpCodes = GlobalUtils.findHashedSystemProperty(
			"b5dd22363f45aa5b98a66928b29049cf25fdb18b3ce467c30ccd71ce5ff43d59ecf8da9182b86f43a3671cf6cba35025337b2352cf6b73cf05ed7e1889f1ca38", "false"
		)
		.equals("true");
	public static final boolean useRandomizedPatternSearch = GlobalUtils.findHashedSystemProperty(
			"3dd773367c0cc8fab6d8f6cd3076485696471c820fad2720719330d69f3aad1b26b2ddb6508cbf8290e2ce43cbf173e919a562e1925f822f69629402dd6910da", "false"
		)
		.equals("true");
	public static final boolean logClassInitOrderErrors = GlobalUtils.findHashedSystemProperty(
			"282399ead2dadcdd327ffcbb43e2efef0629cf8cb992f7409df19033aad339305573ed332613ce7d55984cce4d5abf52fb4856b37143e2adc13848c330fc5ea8", "false"
		)
		.equals("true");
	public static final boolean dontRandomizeFalseDependency = GlobalUtils.findHashedSystemProperty(
			"4dc387c1c1585d696438c659ef2e5083b1c59b2adedcf5306bfe52635b06833321e2d05ed1588039188e7cdc7a25255fef679f272001896269dfc45b0a912c54", "false"
		)
		.equals("true");
	public static final boolean forceFalseDependency = GlobalUtils.findHashedSystemProperty(
			"b17764eb5f734592ca51a2f16d83bb28afcce74f5cb7968d15b939d860c41a788af6a545f72cce09fb187f4439ec5e8a2fca254d8adad32d5797c13a1054643c", "false"
		)
		.equals("true");
	public static final String extraParamProbability = GlobalUtils.findHashedSystemProperty(
		"994a1305d7b40e997279c6e57522eba8c17e1801d3995fff79a7940a9b74ab486f279d112ba90350d7c6e580b29c92a6d0f339abe763bce6ad9c2b79a218c885"
	);
	public static final String patternShuffleProbability = GlobalUtils.findHashedSystemProperty(
		"fc102a8f0a9fc3c71647d2e14ee5999c4eb7606d31969db6278a7d7dfa74741ae48527c23f29f2a2261645ada0ce75e6d2d2a32c98947de723bea9ce85df9e96"
	);
	public static final boolean skipStringConcatAnalysis = GlobalUtils.findHashedSystemProperty(
			"e64f2e57c34f392159095e6ed28ac6509edac31273f5435b3b9eeb535b9bf3a5cd5a1abaf530033367a66b1d4db050b56bf4a686ca713853557d78db64827d0c", "false"
		)
		.equals("true");
	public static final boolean appendExtraSaltTable = GlobalUtils.findHashedSystemProperty(
			"bf4443b85f1f70e0e8618cd0f84e0c14470052e0d0280628edf262929ec5a978bccda3c3f21e763bccc7d0c27d329936fd4e2f3d4e9ad611b443ee898e1f8b0c", "false"
		)
		.equals("true");
	public static final boolean useMaxSaltCount = GlobalUtils.findHashedSystemProperty(
			"b8b60918cbaa5caf81df307fe6e0c7997ac215c32ba763500eef9e683af13eb2fbbee98fa4e38a5644353a7c0c4b12dd78c881bf47cf0a421a11a1c1de16fd2f", "false"
		)
		.equals("true");
	public static final boolean remapBinaryPathNames = GlobalUtils.findHashedSystemProperty(
			"220e5716a59c09ced8c3f09ea365fdf71af013bf53e74c6a77a1f0a104e095f2d79bbd5b0b0e832a15161123a48b705e68ee92ad3e005ad3a434b6ac8c6159d5", "false"
		)
		.equals("true");
	public static final boolean dontRemapInternalPathNames = GlobalUtils.findHashedSystemProperty(
			"05715874bd5ff52265fe6c96b78bf5caadd45816dcc76c923d9e5e5961dddfe606300d966347e3df1b9a1d101e8322565fa4a1ab6107593e7d93e8fec7bd54be", "false"
		)
		.equals("true");
	public static final boolean dontRemapBinaryNames = GlobalUtils.findHashedSystemProperty(
			"84a0f76cc5199a19551dea9a830c15b209243ec6134bd520c70e0e39614b09c6a98f3ce915c1e1cfcb20b843a0e109b393b518a7c49f08a42ea290d729d0252e", "false"
		)
		.equals("true");
	public static final boolean translateKotlinMetadata = GlobalUtils.findHashedSystemProperty(
			"985185e27ed8c496719f4ad43738c62900da7828e968831d958bc1cb130226483661a6d27f53466c722ddeadd480966e9f0e6734a8b1ba8892a4f5ce5a77371d", "false"
		)
		.equals("true");
	public static final boolean logObfuscateRefInclusions = GlobalUtils.findHashedSystemProperty(
			"1d1b979233ffbc9748a34c89e7978e26651815d434c85a75c31c8fa7d5150c77af4cc0216ab7f36fd5dd193c297b4e14ec18db223a39679dbe094419dd9b9118", "false"
		)
		.equals("true");
	public static final boolean sortConstantPoolEntries = GlobalUtils.findHashedSystemProperty(
			"9555750bce6793ea1c683c61087a9b652e16641f3adad684160e1d337a810ce697c57d4d28a390136bc2fda992841241459b2b7229bdd573b31223d385f1181f", "false"
		)
		.equals("true");
	public static final boolean shuffleParameterBlocks = GlobalUtils.findHashedSystemProperty(
			"4cf8f1c175c9df60f5a1feb213ff7b4a264843b1c246dc4bf56e360cce1959fa2cc0fbeb2b37a831a93f4418147b9b4301c943045bc7b27764784edeaec27519", "false"
		)
		.equals("true");
	public static final boolean dontShuffleLocalVariables = GlobalUtils.findHashedSystemProperty(
			"f6a95ae60259beefc27a0d443de8f36ad710dd69972faa873ef25b6c2b979947829ee2024779b50ca558b5acddad368c190725a3c41aa0d1469908d7fc81088e", "false"
		)
		.equals("true");
	public static final boolean dontVerifyStackFrameTypes = GlobalUtils.findHashedSystemProperty(
			"52aa4f663b8797927795af9570477767116b6a40e470ef20ba79b1e0bdee66a8e5d5d662682f762283eaa3444e31d9ecc330ed2a68f7c04ed1cad6b042bfbcc2", "false"
		)
		.equals("true");
	public static final boolean obfuscateInterfaceCalls = GlobalUtils.findHashedSystemProperty(
			"4a2f84b6df74ad5e594b609d277a54a73fcf3a3d4a1f46c4788b499cc380ad1d00f0a54c9ddef93ea0fd93fe79cd3ad4737e5761553f29224b0b6dd662c2cd7b", "false"
		)
		.equals("true");
	public static final boolean unusedOption1 = GlobalUtils.findHashedSystemProperty(
			"d8ea3447df6dcc0d929b94239d43eb7c5f142bd0aa30d7861dc1ada80c9f388b7488a6afc0efb9cf6b2d3c34aa89138d5b5ebcc87fd5d7d982d28ad857ead68d", "false"
		)
		.equals("true");
	public static final boolean forceExtraParams = GlobalUtils.findHashedSystemProperty(
			"fd24edc204d9a66eab01d90996ce3405c55ea939913d7bc56cff2006529b5ec98b8b0a44b623b6e509636ecbcef10476355857edec54faf91a4e996c7501d43c", "false"
		)
		.equals("true");
	public static final boolean unusedOption2 = GlobalUtils.findHashedSystemProperty(
			"a0db2b89dd0c02e61713795fa3c78b138fa79e7bb289ff276fb7af4df77fba7b0923e4962895e12052ea70f9d7564a6604977f296c8c63ca6d2d84297b26899a", "false"
		)
		.equals("true");
	public static final boolean useDotIndentSuffix = GlobalUtils.findHashedSystemProperty(
			"c49ea278096489d21009bcd355d748a51642b8f15152c2fbe131f732d9c4e2a7bd81586b5267cee75b193deaa0fc58474ceb791310e1cacaa836f4572f564f4a", "false"
		)
		.equals("true");
	public static final boolean dontShuffleNameAlphabet = GlobalUtils.findHashedSystemProperty(
			"ead1536adffbcb844ac97dbedc038b74d80f87cd58d74913acd5386ee9619f34c5c029eeb449569ff0efd6035b1ba1bc0bd8613c644be09ca5b4703c5aedc137", "false"
		)
		.equals("true");
	public static final boolean keepRealTypesInDescriptors = GlobalUtils.findHashedSystemProperty(
			"6f69d473f4ff6a65f032855748b6f2750f8ab2eef638fd42c2b79c6f386b5668064244520658774571f363ddfd1ce4facd465b11ac2edb41fb8c1a3075183089", "false"
		)
		.equals("true");
	public static final boolean noUnderscoreInMethodNames = GlobalUtils.findHashedSystemProperty(
			"9f52dfb2107da33ce1959acc32c1dba5f72dd48dc6634cb2eb619b0508d1c13c3fea4c7fc3f7b652c9ddaacf21cd7825d928ae6331f21d1a61b14b84cf3b8cb5", "false"
		)
		.equals("true");
	public static final boolean noUnderscoreInFieldNames = GlobalUtils.findHashedSystemProperty(
			"26b43016732f031e4e520f75fb45862025d12559d5d277204717201acf6404984ec9dec090247bd625a64a608b54534c22868c8d91ee69850ef36d05d04494f7", "false"
		)
		.equals("true");
	public static final boolean lightMpcFlowObfuscation = GlobalUtils.findHashedSystemProperty(
			"8402c65c1b1177c2715b2aa2f9d226b3bb1aef941f423fcf5389329613727faa6554c5bb875f600aeceda66fb80937dc8d8e7dd5c35b891c1e52cebd27bbca45", "false"
		)
		.equals("true");
	public static final boolean dontEncryptIntConstants = GlobalUtils.findHashedSystemProperty(
			"0780bf5c91372bfc86dac7e6f3fbe503ea9338de0e3f9a9e78f1ecbbe596052346bf2930ac661a135ae1086a710db1822387a6feeeea7746739098bd19625913", "false"
		)
		.equals("true");
	public static final boolean forceEncryptIntConstants = GlobalUtils.findHashedSystemProperty(
			"3f0156e9eed9e1c4d84d9edd2f7f3cda3fa2236f016be4bbcb3defcd481b311d1720a03754b0cbdb5230e684530617158df51d318b9b56e051e57d2f1991956d", "false"
		)
		.equals("true");
	public static final boolean dontUseSecureRandom = GlobalUtils.findHashedSystemProperty(
			"0b006e2473e6274515751d6345b4e81e6d736224d76a922236208715a841c38a0e5555299dd54f0ac0353608343351553414d18bfa4a1f5187f7fabbee4e2b07", "false"
		)
		.equals("true");
	public static final boolean dontSplitColonNames = GlobalUtils.findHashedSystemProperty(
			"7417f80dba9cff2f6cdbdbdadae00ce5b95e52cb04dfffd3943ef1d4231c35fb689eddd5e5d3e391fa033bd8ea10259df61668b21f9c52bd6a1aadd4f1efbfe9", "false"
		)
		.equals("true");
	public static final boolean keepOriginalSourceFileName = GlobalUtils.findHashedSystemProperty(
			"860fc9f771c65de2350b4544ea20118f02c6413a214f81d9dc1725538eb6e43d3a602a03c73c8dd9960179aa70bb314c77f753f2cf92fcb5e67376648a86b765", "false"
		)
		.equals("true");
	public static final boolean forceRelaxStackFrameCompatibility = GlobalUtils.findHashedSystemProperty(
			"fab2a78f5ccaec523f57347aa6a501ef92c37c5ce78b1b5c099051b55c3a7e1cc45174fce003812e485279b6176d914506d1363eb3ca56128fc867efb5a9f607", "false"
		)
		.equals("true");
	public static final boolean requireStrictStackFrameCompatibility = GlobalUtils.findHashedSystemProperty(
			"74f05f336c4afb7a87156697bdbaf511b7cc23515f58d13bfd16b8c2e0a3bc7675da308dc9c6701c55aecde254e080e5d49522beadde368301449a82ef476e46", "false"
		)
		.equals("true");
//   public static final boolean O0 = LogUtils.Method1850(
//      )
//      .equals("true");
	public static final boolean objectAssignableToInterfaces = GlobalUtils.findHashedSystemProperty(
			"1d9510305209f923a9d35ee347fddc65625bf1475c34cb82fd1aa09b6ad3a6e541602537836f841b87be1412aa7a99b2dcc4d009162d83797f6a2a8f835edef2", "false"
		)
		.equals("true");
//   public static final boolean Om = LogUtils.Method1850(
//      )
//      .equals("true");
	public static final boolean dontInferTransitiveClassInitOrder = GlobalUtils.findHashedSystemProperty(
			"7dc6efc3c3ef28593ba89ebfcb05c2742f16558003b59124e5d7953a5e680474a78173a12b18d1fe3ca96f0b7dcc7058ac40e354a6d5d216f1812e86ffb6003b", "false"
		)
		.equals("true");
	public static final boolean useLogErrorForRenameFilter = GlobalUtils.findHashedSystemProperty(
					"d2d927125edc45ae38eaf3f5a35b8b5312e267fce1f88b3edcf693df571c94af80cace62a186b2525859a0fade00ac301997b7ac4e48bc0a984a5a6dd63b1b73", "false"
			)
			.equals("true");
	public static final boolean registerMissingClassFieldRefs = GlobalUtils.findHashedSystemProperty(
					"a5a49acffc6c0c0444488ce06cab8bab5940c2f828cc49a209e74132fa94289e48a63f8f42884fc6b38f925426e862206cadb9c1b35ffb12225e32c2702ef514", "false"
			)
			.equals("true");
public static final boolean registerRootClassFieldRefs = GlobalUtils.findHashedSystemProperty(
				"98eb1c2616ed93c9c27ec1cf890873f98a1b7d3f0b33352b29c9268fc41e688430981f941f0dd98d51444c5e8dbd949ac280ba4152a3cb165ccff19c9370a493", "false"
		)
		.equals("true");
	public static final boolean dontRandomizeFlowObfuscation = GlobalUtils.findHashedSystemProperty(
					"501b3d1eb361542e7d57020ad76fe4a8509f44f1edf225a5a6502db3b3edbadfb222905243ef7f6e11eb2ba5bdd393d4b510959c9cbf6f36dd47c73a5a974286", "false"
			)
			.equals("true");
	public static final boolean dontRandomizeFlowObfuscationWithSuccessor = GlobalUtils.findHashedSystemProperty(
					"ca452adb792254cf344dd06ec334755bcd63048ecf35bde962ebf786ccf890df69216444d833c9015113ff8363f19e3ce778b99de130ff1e6e08e96c8072ac4c", "false"
			)
			.equals("true");
	public static final boolean dontRelocateFlowPatchLoad = GlobalUtils.findHashedSystemProperty(
					"b2646b23f419d82ffad4d8ecc7712538e0a0198aa0b6a2828331b5f1b3a299b1e60d8c15cb455dcb3a2cafca9384be9c9aebcc88210fb5d06d81a98220afab70", "false"
			)
			.equals("true");
//   public static final boolean OO = LogUtils.Method1850(
//      )
//      .equals("true");
public static final boolean skipNameListUnescape = GlobalUtils.findHashedSystemProperty(
				"9a0be765c0015d5e883f44777725042018960a597afedf852fe7fb0ad6bb31f0194a6f1f5fb2440d5f27437c70b576fb15d7be51f826a3a730d777b7aac572f8", "false"
		)
		.equals("true");
	public static final boolean validateNameListIdentifiers = GlobalUtils.findHashedSystemProperty(
					"b1bb235d591a53c2f0f668d985046534136169ac18d138949e12bbe6872141ae8d1d208efd3db580a918ead6d8b48f3b8533948ff7f936adc1e3cb017b53d26a", "false"
			)
			.equals("true");
	public static final boolean skipParameterUnpackingAnalysis = GlobalUtils.findHashedSystemProperty(
					"97a01359db3418c74c1eb353d55dcb74e3c577ae99f473f19306f55dcd2f717999cb10360608267ffd4301ea4883d3646ef7f0493ee1a6127fbbae29907aba4c", "false"
			)
			.equals("true");
//   public static final boolean li = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean OF = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean h8 = LogUtils.Method1850(
//      )
//      .equals("true");
//   public static final boolean lb = LogUtils.Method1850(
//      )
//      .equals("true");
}
