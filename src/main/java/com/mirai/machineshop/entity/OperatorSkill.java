package com.mirai.machineshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "operator_skills")
public class OperatorSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @Column(nullable = false)
    private String skillName;

    public OperatorSkill() {
    }

    public OperatorSkill(Operator operator, String skillName) {
        this.operator = operator;
        this.skillName = skillName;
    }

    public Long getId() {
        return id;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }
}