package org.jenkinsci.plugins.jobdsl.promotions

import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.helpers.Context
import com.google.common.base.Preconditions
import javaposse.jobdsl.dsl.helpers.AbstractContextHelper

class PromotionsContext implements Context {

    JobManagement jobManagement

    List<Node> promotionNodes = []

    Map<String, Node> subPromotionNodes = [:]

    List<String> names = []

    PromotionsContext(JobManagement jobManagement) {
        this.jobManagement = jobManagement
    }

    /**
     * PromotionNodes:
     * 1. <string>dev</string>
     * 2. <string>test</string>
     * 
     * AND
     * 
     * Sub PromotionNode for every promotion
     * 1. <project>
     *     <name>dev</name>
     *     .
     *     .
     *     .
     * </project>
     * 2. <project>
     *     <name>test</name>
     *     .
     *     .
     *     .
     * </project>
     * 
     * @param promotionClosure
     * @return
     */
    def promotion(Closure promotionClosure = null) {
        PromotionContext promotionContext = new PromotionContext(jobManagement)
        AbstractContextHelper.executeInContext(promotionClosure, promotionContext)

        Preconditions.checkNotNull(promotionContext.name, 'promotion name cannot be null')
        Preconditions.checkArgument(promotionContext.name.length() > 0)

        def name = promotionContext.name
        def promotionNode = new Node(null, 'string', name)

        def subPromotionNode = new NodeBuilder().'project' {
            // Conditions to proof before promotion
            if (promotionContext.conditions) {
                promotionContext.conditions.each {ConditionsContext condition ->
                    conditions(condition.createConditionNode().children())
                }
            }

            // Icon, i.e. star-green
            if (promotionContext.icon) {
                icon(promotionContext.icon)
            }

            // Restrict label
            if (promotionContext.restrict) {
                assignedLabel(promotionContext.restrict)
            }
        }

        // Actions for promotions ... BuildSteps
        def steps = new NodeBuilder().'buildSteps'()
        if (promotionContext.actions) {
            promotionContext.actions.each { steps.append(it) }
        }
        subPromotionNode.append(steps)

        // Fill Lists and Maps
        names << name
        promotionNodes << promotionNode
        subPromotionNodes.put(name, subPromotionNode)
    }

}
