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
                        
                        private String getMagicWord1(int a, int b){
                            int test = 0;
                            int c = a;
                            return magicWord1;
                        }

                        private String getMagicWord2(int a, int b){
                            int test = instanceData2;
                            int c = a;
                            return magicWord2;
                        }
                        
                        @Override
                        private String getMagicWord3(int a, int b){
                            int test = 0;
                            int c = a;
                            return magicWord1;
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
                        
                        private static String getMagicWord1(int a, int b){
                            int test = 0;
                            int c = a;
                            return magicWord1;
                        }

                        private String getMagicWord2(int a, int b){
                            int test = instanceData2;
                            int c = a;
                            return magicWord2;
                        }
                        
                        @Override
                        private String getMagicWord3(int a, int b){
                            int test = 0;
                            int c = a;
                            return magicWord1;
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
