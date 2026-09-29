package com.barbonperformance.analysis.strength.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "red_flag")
public class RedFlag {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private PlayerProfile player;

    @Column(nullable = false)
    private String title;

    @Column
    private String body;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected RedFlag() {
        // for JPA
    }

    private RedFlag(Organization organization, PlayerProfile player, String title, String body,
            int sortOrder) {
        this.organization = organization;
        this.player = player;
        this.title = title;
        this.body = body;
        this.sortOrder = sortOrder;
    }

    public static RedFlag forEveryone(Organization organization, String title, String body, int sortOrder) {
        return new RedFlag(organization, null, title, body, sortOrder);
    }

    public static RedFlag forPlayer(PlayerProfile player, String title, String body, int sortOrder) {
        return new RedFlag(player.getOrganization(), player, title, body, sortOrder);
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
