require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-openfoodfacts"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]
  s.platforms    = { :ios => "15.1" }
  s.source       = { :git => "https://github.com/openfoodfacts/openfoodfacts-react-native.git", :tag => s.version.to_s }
  s.source_files = "ios/**/*.{h,mm,swift}"
  s.requires_arc = true

  s.dependency "React-Core"

  if ENV["RCT_NEW_ARCH_ENABLED"] == "1"
    s.compiler_flags = "-DRCT_NEW_ARCH_ENABLED=1"
    s.pod_target_xcconfig = {
      "DEFINES_MODULE" => "YES",
      "CLANG_CXX_LANGUAGE_STANDARD" => "c++20"
    }
    s.dependency "ReactCodegen"
    s.dependency "RCTRequired"
    s.dependency "RCTTypeSafety"
    s.dependency "ReactCommon/turbomodule/core"
  end
end
