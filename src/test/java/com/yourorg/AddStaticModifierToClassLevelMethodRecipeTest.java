package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddStaticModifierToClassLevelMethodRecipeTest implements RewriteTest {

    //Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    //In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    //per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddStaticModifierToClassLevelMethodRecipe("com.yourorg.A"))
            .parser(JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .classpath("guava"));
    }

    @Test
    void addStaticModifierToClassLevelMethod() {
        rewriteRun(
            java(
                """
                    package com.yourorg;

                    class A {
                        private static String magicWord1 = "magic1";
                        private static String magicWord2 = "magic2";

                        private String instanceData1 = "test1";
                        String instanceData2 = "test2";
                        
                        public A () {

                        }

                        // nonStaticMethod1, nonStaticMethod2, nonStaticMethod3 can be converted to static methods transitively
                        public int nonStaticMethod1 (){
                            nonStaticMethod1();
                            return magicWord1;
                        }

                        public int nonStaticMethod2 (){
                            return nonStaticMethod1();
                        }

                        public int nonStaticMethod3 (){
                            return nonStaticMethod2();
                        }

                        public abstract int abstractMethod(int n1, int n2);

                        private String getMagicWord1(int a, int b){
                            int test = 0;
                            int c = a;
                            return magicWord1;
                        }

                        private String getMagicWord2(int a, int b){
                            int test = this::instanceData2;
                            int c = a;
                            return magicWord2;
                        }
                        
                        @Override
                        private String getMagicWord3(int a, int b){
                            int test = 0;
                            int c = a;
                            return magicWord1;
                        }

                        private String getMagicWord4(int a, int b){
                            super.test();
                            return magicWord1;
                        }

                        public class B {
                            public B() {}
                            private String getMagicWord5() {
                                return "";
                            }
                        }
                    }
                """
                ,
                """
                    package com.yourorg;

                    class A {
                        private static String magicWord1 = "magic1";
                        private static String magicWord2 = "magic2";

                        private String instanceData1 = "test1";
                        String instanceData2 = "test2";
                        
                        public A () {

                        }

                        // nonStaticMethod1, nonStaticMethod2, nonStaticMethod3 can be converted to static methods transitively
                        public static int nonStaticMethod1 (){
                            nonStaticMethod1();
                            return magicWord1;
                        }

                        public static int nonStaticMethod2 (){
                            return nonStaticMethod1();
                        }

                        public static int nonStaticMethod3 (){
                            return nonStaticMethod2();
                        }

                        public abstract int abstractMethod(int n1, int n2);

                        private static String getMagicWord1(int a, int b){
                            int test = 0;
                            int c = a;
                            return magicWord1;
                        }

                        private String getMagicWord2(int a, int b){
                            int test = this::instanceData2;
                            int c = a;
                            return magicWord2;
                        }
                        
                        @Override
                        private String getMagicWord3(int a, int b){
                            int test = 0;
                            int c = a;
                            return magicWord1;
                        }

                        private String getMagicWord4(int a, int b){
                            super.test();
                            return magicWord1;
                        }

                        public class B {
                            public B() {}
                            private String getMagicWord5() {
                                return "";
                            }
                        }
                    }
                """
            )
        );
    }

    @Test
    void sonarSourceExample() {
        rewriteRun(
            java(
                """
                    package com.yourorg;
        
                    class A {

                        private static String magicWord = "magic";

                        private String getMagicWord () {
                            return magicWord;
                        }

                        private void setMagicWord(String v){
                            magicWord = v;
                        }
                    }
                """,
                """
                    package com.yourorg;
        
                    class A {

                        private static String magicWord = "magic";

                        private static String getMagicWord () {
                            return magicWord;
                        }

                        private static void setMagicWord(String v){
                            magicWord = v;
                        }
                    }
                """
            )
        );
    }
}
