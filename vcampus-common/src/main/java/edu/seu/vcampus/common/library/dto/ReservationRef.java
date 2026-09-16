package edu.seu.vcampus.common.library.dto;

import java.io.Serializable;

/** 指向一条预约记录的协议参数。 */
public class ReservationRef implements Serializable {
    private static final long serialVersionUID = 1L;
    private long reservationId;

    /** 创建空参数。 */
    public ReservationRef() {
    }

    /** @param reservationId 预约记录号 */
    public ReservationRef(long reservationId) {
        this.reservationId = reservationId;
    }

    /** @return 预约记录号 */
    public long getReservationId() {
        return reservationId;
    }

    /** @param reservationId 预约记录号 */
    public void setReservationId(long reservationId) {
        this.reservationId = reservationId;
    }
}
