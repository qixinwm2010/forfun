package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SayHelloRecipeTest implements RewriteTest {

    //Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    //In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    //per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SayHelloRecipe("com.yourorg.A"))
            .parser(JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .classpath("guava"));
    }

    @Test
    void addsHelloToA() {
        rewriteRun(
            //There is an overloaded version or rewriteRun that allows the RecipeSpec to be customized specifically
            //for a given test. In this case, the parser for this test is configured to not log compilation warnings.
            spec -> spec
                .parser(JavaParser.fromJavaVersion()
                    .logCompilationWarningsAndErrors(false)
                    .classpath("guava")),
                    java(
                        """
                            package com.yourorg;
                
                            class A {
                            }
                        """,
                        """
                            package com.yourorg;
                
                            class A {
                                public String hello() {
                                    return "Hello from com.yourorg.A!";
                                }
                            }
                        """
                    )
        );
    }
    @Test
    void doesNotChangeExistingHello() {
        rewriteRun(
            java("""
                    package com.yourorg;
        
                    class A {
                        public String hello() { return ""; }
                    }
                """
            )
        );
    }
    @Test
    void doesNotChangeOtherClass() {
        rewriteRun(
            java( 
                """
                    package com.yourorg;
        
                    class B {
                    }
                """
            )
        );
    }
}
