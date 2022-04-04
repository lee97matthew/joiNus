/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ejb.session.stateless;

import entity.ActivityEntity;
import entity.BookingEntity;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import util.exception.InputDataValidationException;
import util.exception.UnknownPersistenceException;

/**
 *
 * @author User
 */
@Stateless
public class BookingEntitySessionBean implements BookingEntitySessionBeanLocal {

    @PersistenceContext(unitName = "joiNus-ejbPU")
    private EntityManager em;

    private final ValidatorFactory validatorFactory;
    private final Validator validator;

    public BookingEntitySessionBean() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    @Override
    public BookingEntity createNewBooking(BookingEntity newBookingEntity) throws UnknownPersistenceException, InputDataValidationException {
        Set<ConstraintViolation<BookingEntity>> constraintViolations = validator.validate(newBookingEntity);

        if (constraintViolations.isEmpty()) {
            try {
                em.persist(newBookingEntity);
                //set linkages
                em.flush();

                return newBookingEntity;
            } catch (PersistenceException ex) {
                if (ex.getCause() != null && ex.getCause().getClass().getName().equals("org.eclipse.persistence.exceptions.DatabaseException")) {
                    throw new UnknownPersistenceException(ex.getMessage());
                } else {
                    throw new UnknownPersistenceException(ex.getMessage());
                }
            }
        } else {
            throw new InputDataValidationException(prepareInputDataValidationErrorsMessage(constraintViolations));
        }
    }

    @Override
    public List<BookingEntity> retrieveBookingByActivity(Long activityId) {
        Query query = em.createQuery("SELECT b FROM BookingEntity b WHERE b.activity.activityId = :activityId BY b.bookingId ASC");

        query.setParameter("activityId", activityId);
        List<BookingEntity> bookingEntities = query.getResultList();

        for (BookingEntity bookingEntity : bookingEntities) {
            bookingEntity.getActivity();
            bookingEntity.getTimeSlot();
        }

        return bookingEntities;
    }
    
    @Override
    public void associateBookingWithActivity(Long bookingId, Long activityId) {
        BookingEntity booking = em.find(BookingEntity.class, bookingId);
        ActivityEntity activity = em.find(ActivityEntity.class, activityId);
        
        booking.setActivity(activity);
        activity.setBooking(booking);
       
    }

    private String prepareInputDataValidationErrorsMessage(Set<ConstraintViolation<BookingEntity>> constraintViolations) {
        String msg = "Input data validation error!:";

        for (ConstraintViolation constraintViolation : constraintViolations) {
            msg += "\n\t" + constraintViolation.getPropertyPath() + " - " + constraintViolation.getInvalidValue() + "; " + constraintViolation.getMessage();
        }

        return msg;
    }

}
