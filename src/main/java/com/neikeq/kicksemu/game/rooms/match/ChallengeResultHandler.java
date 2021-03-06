package com.neikeq.kicksemu.game.rooms.match;

import com.neikeq.kicksemu.game.clubs.ClubInfo;
import com.neikeq.kicksemu.game.rooms.ChallengeRoom;
import com.neikeq.kicksemu.game.rooms.ClubRoom;
import com.neikeq.kicksemu.game.rooms.Room;
import com.neikeq.kicksemu.game.rooms.challenges.Challenge;
import com.neikeq.kicksemu.network.packets.out.MessageBuilder;
import com.neikeq.kicksemu.storage.ConnectionRef;

import java.sql.SQLException;

public class ChallengeResultHandler extends MatchResultHandler {

    private final Challenge challenge;

    public ChallengeResultHandler(Room room, MatchResult result, ConnectionRef connection) throws SQLException {
        super(room, result, connection);
        challenge = ((ChallengeRoom) room).getChallenge();
    }

    @Override
    public void handleResult() {
        updateTeamWins();
        super.handleResult();
        rewardClubs();
    }

    @Override
    protected void applyRewards() {
        getResult().getPlayers().forEach(playerResult -> {
            ClubPlayerRewards playerRewards = new ClubPlayerRewards(this, playerResult);
            playerRewards.applyMatchRewards();
        });
    }

    private void updateTeamWins() {
        updateTeamWins(challenge.getRedTeam(), getResult().getRedTeam());
        updateTeamWins(challenge.getBlueTeam(), getResult().getBlueTeam());

        getRoom().broadcast(MessageBuilder.challengeUpdateWins(challenge));
    }

    private void updateTeamWins(ClubRoom team, TeamResult teamResult) {
        switch (teamResult.getResult()) {
            case WIN:
                team.setTotalWins((byte) (team.getTotalWins() + 1));
                team.setWinStreak((byte) (team.getWinStreak() + 1));
                break;
            case DRAW:
            case LOSE:
                team.setWinStreak((byte) 0);
                break;
        }
    }

    private void rewardClubs() {
        rewardClub(challenge.getRedTeam(), getClubReward(getResult().getRedTeam()));
        rewardClub(challenge.getBlueTeam(), getClubReward(getResult().getBlueTeam()));
    }

    private void rewardClub(ClubRoom clubRoom, int clubPoints) {
        int clubId = clubRoom.getId();
        ClubInfo.sumClubPoints(clubPoints, clubId, getConnection());
        clubRoom.broadcast(MessageBuilder.clubInfo(clubId));
    }

    private int getClubReward(TeamResult teamResult) {
        switch (teamResult.getResult()) {
            case WIN:
                return 100;
            case DRAW:
                return 50;
            case LOSE:
                return 30;
            default:
                return 0;
        }
    }
}
