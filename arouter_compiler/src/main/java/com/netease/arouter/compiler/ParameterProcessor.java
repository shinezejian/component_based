package com.netease.arouter.compiler;

import com.google.auto.service.AutoService;
import com.netease.arouter.annotation.Parameter;
import com.netease.arouter.compiler.utils.Constants;
import com.netease.arouter.compiler.utils.EmptyUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)//编译版本
@SupportedAnnotationTypes({Constants.AROUTER_ANNOTATION_TYPES_PARAMTER})//支持的注解类型，全路径名称
public class ParameterProcessor extends AbstractProcessor {
    //Element可以是类、方法、变量等代表的是源代码。
    // TypeElement代表的是源代码中的类型元素，例如类,接口，函数等。然而，
    // TypeElement并不包含类本身的信息。你可以从TypeElement中获取类的名字，
    // 但是你获取不到类的信息，例如它的父类。这种信息需要通过TypeMirror获取。
    // 你可以通过调用elements.asType()获取元素的TypeMirror。
    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementUtils;

    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;

    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;

    // 文件生成器 类/资源，Filter用来创建新的类文件，class文件以及辅助文件
    private Filer filer;

    // 临时map存储，用来存放被@Parameter注解的属性集合，生成类文件时遍历
    // key:类节点, value:被@Parameter注解的属性集合
    private Map<TypeElement, List<Element>> tempElementMap = new HashMap<>();
    // 获取元素接口信息（生成类文件需要的接口实现类）
    private TypeMirror callMirror;

    /**
     * 初始化参数获取工具或者额外参数
     *
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        // 在gradle文件中配置选项参数值（用于APT传参接收）
        // 切记：必须写在defaultConfig节点下
        // javaCompileOptions {
        //     annotationProcessorOptions {
        //         arguments = [moduleName: project.getName(), packageNameForAPT: packageNameForAPT]
        //     }
        // }
        //获取额外参数可以在gradle中设置
        Map<String, String> options = processingEnvironment.getOptions();
        //k获取Call 类型的接口信息
        callMirror = elementUtils.getTypeElement(Constants.CALL).asType();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {


        if (!EmptyUtils.isEmpty(set)) {
            //1.获取被注解的元素
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Parameter.class);

            try {
                //保存每个被@Paramter注解的变量，方便后续生成文件
                saveTempElementValue(elements);
                createParameterFile();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return false;
    }

    /**
     * 生成文件
     */
    private void createParameterFile() throws IOException {

        if (EmptyUtils.isEmpty(tempElementMap)) {
            return;
        }

        // 通过Element工具类，获取Parameter类型
        //获取acitivity类型用于判断@Parameter是否在Activity中使用
        TypeElement activityType = elementUtils.getTypeElement(Constants.ACTIVITY);
        //parameterType注解对应生成类要实现的接口
        TypeElement parameterType = elementUtils.getTypeElement(Constants.PARAMETER_LOAD);

        //构建返回参数-》Object target
        ParameterSpec parameterSpec = ParameterSpec.builder(TypeName.OBJECT, Constants.PARAMETER_NAMR).build();

        for (Map.Entry<TypeElement, List<Element>> entry : tempElementMap.entrySet()) {
            //key 代表的是类名，比如MainActivity
            TypeElement keyType = entry.getKey();
            messager.printMessage(Diagnostic.Kind.NOTE, "keyType=" + keyType.toString() + ",activityType=" + activityType);

            //注意keyType 与 activityType位置不可调换
            if (!typeUtils.isSubtype(keyType.asType(), activityType.asType())) {
                // 如果类名的类型和Activity类型不匹配
                throw new RuntimeException("@Parameter注解目前仅限用于Activity类之上");
            }

//            public class Order_DetailActivity$$Parameter implements ParameterLoad {
//                @Override
//                public void loadParameter(Object target) {
//                    Order_DetailActivity t = (Order_DetailActivity)target;
//                    t.username = t.getIntent().getStringExtra("username");
//                }
//            }
            //获取类名称
            ClassName classType = ClassName.get(keyType);

            //构建方法体
            MethodSpec.Builder methodBuild = MethodSpec.methodBuilder(Constants.PARAMETER_METHOD_NAME)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(parameterSpec);

            methodBuild.addStatement("$T t = ($T)target", classType, classType);

            // 遍历使用了该注解当前类里面所有属性
            for (Element element : entry.getValue()) {
                addMethodStatement(element, methodBuild);
            }

            //最终生成的类名文件（）
            String finalClassName = classType.simpleName() + Constants.PARAMETER_FILE_NAME;
            messager.printMessage(Diagnostic.Kind.NOTE, "APT生成获取参数类文件：" +
                    classType.packageName() + "." + finalClassName);
            //生成文件
            // MainActivity$$Parameter
            JavaFile.builder(classType.packageName(), // 包名
                    TypeSpec.classBuilder(finalClassName) // 类名
                            .addSuperinterface(ClassName.get(parameterType)) // 实现ParameterLoad接口
                            .addModifiers(Modifier.PUBLIC) // public修饰符
                            .addMethod(methodBuild.build()) // 方法的构建（方法参数 + 方法体）
                            .build()) // 类构建完成
                    .build() // JavaFile构建完成
                    .writeTo(filer); // 文件生成器开始生成类文件
        }

    }

    private void addMethodStatement(Element element, MethodSpec.Builder methodBuild) {

        //获取element的属性
        TypeMirror typeMirror = element.asType();

        //获取被注解元素的类型的枚举下标值
        int index = typeMirror.getKind().ordinal();

        //获取属性名称
        String fieldName = element.getSimpleName().toString();

        //获取被注解元素的值(传递时的参数名称，如getBooleanExtra("isSuccess", t.age)的“isSuccess”)
        String annotationValue = element.getAnnotation(Parameter.class).name();
        // 判断注解的值为空的情况下的处理（注解中有name值就用注解值）
        annotationValue = EmptyUtils.isEmpty(annotationValue) ? fieldName : annotationValue;

        // 最终拼接的前缀：
        String finalValue = "t." + fieldName;
        // t.s = t.getIntent().
        String methodContent = finalValue + "= t.getIntent().";

        // TypeKind 枚举类型不包含String
        if (index == TypeKind.INT.ordinal()) {
            // t.s = t.getIntent().getIntExtra("age", t.age);
            methodContent += "getIntExtra($S, " + finalValue + ")";
        } else if (index == TypeKind.BOOLEAN.ordinal()) {
            // t.s = t.getIntent().getBooleanExtra("isSuccess", t.age);
            methodContent += "getBooleanExtra($S," + finalValue + ")";
        } else {
            // t.s = t.getIntent.getStringExtra("s");
            if (typeMirror.toString().equalsIgnoreCase(Constants.STRING)) {
                methodContent += "getStringExtra($S)";
            } else if (typeUtils.isSubtype(typeMirror, callMirror)) {
                // 类型工具类方法isSubtype，相当于instance一样
                //t.orderAddress = (OrderAddress) RouterManager.getInstance().build("/order/getOrderBean").navigation(t);
                methodContent = "t." + fieldName + " = ($T) $T.getInstance().build($S).navigation(t)";
                methodBuild.addStatement(methodContent,
                        TypeName.get(typeMirror),
                        ClassName.get(Constants.BASE_PACKAGE, Constants.ROUTER_MANAGER),
                        annotationValue);
                return;
            }
        }

        // 健壮代码
        if (methodContent.endsWith(")")) {
            // 添加最终拼接方法内容语句
            methodBuild.addStatement(methodContent, annotationValue);
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, "目前暂支持String、int、boolean传参");
        }

    }

    // 临时map存储，用来存放被@Parameter注解的属性集合，生成类文件时遍历
    // key:类节点, value:被@Parameter注解的属性集合
    private void saveTempElementValue(Set<? extends Element> elements) {
        for (Element element : elements) {
            //获取当前被@paramter参数修饰的字段的父类节点
            TypeElement superElement = (TypeElement) element.getEnclosingElement();

            if (tempElementMap.containsKey(superElement)) {
                tempElementMap.get(superElement).add(element);
            } else {
                List<Element> list = new ArrayList<>();
                list.add(element);
                tempElementMap.put(superElement, list);
            }
        }
    }

    /**
     * 获取额外参数
     *
     * @return
     */
    @Override
    public Set<String> getSupportedOptions() {
        return super.getSupportedOptions();
    }

    /**
     * 获取注解支持的类型，一般是注解的全路径名
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return super.getSupportedAnnotationTypes();
    }

    /**
     * 获取编译版本
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return super.getSupportedSourceVersion();
    }
}
