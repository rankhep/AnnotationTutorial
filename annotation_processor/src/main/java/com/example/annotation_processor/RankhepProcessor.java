package com.example.annotation_processor;

import com.example.annotaion.RankhepIntent;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public class RankhepProcessor extends AbstractProcessor {

    private static final ClassName intentClass = ClassName.get("android.content", "Intent");
    private static final ClassName contextClass = ClassName.get("android.content", "Context");
    private static final String METHOD_PREFIX_NEW_INTENT = "intentFor";

    ArrayList<MethodSpec> newIntentMethodSpecs = new ArrayList<>();

    private String packageName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //프로세싱에 필요한 기본적인 정보들을 processingEnvironment 부터 가져올 수 있습니다.
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        System.out.println("애노테이션 프로세싱!!");//프로세싱이 되는지 확인하기 위한 로그 확인용입니다.
        final Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(RankhepIntent.class);

        for (Element element : elements) {
            if(packageName==null){
                //패키지 명이 null일때 엘리먼트의 패키지 네임을 가져와 저장한다.
                Element e = element;
                while (!(e instanceof PackageElement)) {
                    e = e.getEnclosingElement();
                }
                packageName = ((PackageElement)e).getQualifiedName().toString();
            }

            if (element.getKind() != ElementKind.CLASS) {
                //어노테이션이 적용된 엘리먼트의 타입을 확인해서
                //Class가 아닐 때 false를 리턴한다.
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "RankhepIntent는 Class에만 적용 가능합니다.");
                return false;
            }
            newIntentMethodSpecs.add(generateMethod((TypeElement) element));
        }

        if (roundEnvironment.processingOver()) {
            try {
                generateJavaFile(newIntentMethodSpecs);
                return true;
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.toString());
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>(){
            {
                add(RankhepIntent.class.getCanonicalName());// 어떤 애노테이션을 처리할 지 Set에 추가합니다.
            }
        };
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();//지원되는 소스 버전을 리턴합니다.
    }




    private MethodSpec generateMethod(TypeElement element) {
        return MethodSpec
                .methodBuilder(METHOD_PREFIX_NEW_INTENT + element.getSimpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(contextClass, "context")
                .returns(intentClass)
                .addStatement("return new $T($L, $L)", intentClass, "context", element.getQualifiedName() + ".class")
                .build();
        //Method 작성 시 메소드의 스펙을 지정하는 함수
    }

    //작성한 메소드 스펙의 자바 파일을 생성하는 함수
    private void generateJavaFile(List<MethodSpec> methodSpecList) throws IOException {
        System.out.println("methodSpecList Count = "+methodSpecList.size());
        final TypeSpec.Builder builder = TypeSpec.classBuilder("Rankhep");
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        for (MethodSpec methodSpec : methodSpecList) {
            builder.addMethod(methodSpec);
        }

        final TypeSpec typeSpec = builder.build();

        JavaFile.builder(packageName, typeSpec)
                .build()
                .writeTo(processingEnv.getFiler());
    }

}